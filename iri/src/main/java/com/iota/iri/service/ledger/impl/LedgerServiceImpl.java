package com.iota.iri.service.ledger.impl;

import com.iota.iri.BundleValidator;
import com.iota.iri.controllers.MilestoneViewModel;
import com.iota.iri.controllers.StateDiffViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.ledger.LedgerException;
import com.iota.iri.service.ledger.LedgerService;
import com.iota.iri.service.milestone.MilestoneService;
import com.iota.iri.service.snapshot.Snapshot;
import com.iota.iri.service.snapshot.SnapshotException;
import com.iota.iri.service.snapshot.SnapshotProvider;
import com.iota.iri.service.snapshot.SnapshotService;
import com.iota.iri.service.snapshot.impl.SnapshotStateDiffImpl;
import com.iota.iri.service.spentaddresses.SpentAddressesService;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.Converter;
import com.iota.iri.utils.dag.DAGHelper;

import com.iota.iri.vm.Repository;
import com.iota.iri.vm.config.CommonConfig;
import com.iota.iri.vm.program.invoke.ProgramInvokeFactory;
import com.iota.iri.vm.program.invoke.ProgramInvokeFactoryImpl;
import com.iota.iri.vm.TransactionExecutionSummary;
import com.iota.iri.vm.AccountMap;
import com.iota.iri.model.persistables.Transaction;
import com.iota.iri.TransactionValidator;
import com.iota.iri.conf.APIConfig;
import com.iota.iri.conf.IotaConfig;
import com.iota.iri.controllers.*;
import com.iota.iri.crypto.PearlDiver;
import com.iota.iri.crypto.Sponge;
import com.iota.iri.crypto.SpongeFactory;
import com.iota.iri.model.Hash;
import com.iota.iri.model.HashFactory;
import com.iota.iri.model.persistables.Transaction;
import com.iota.iri.network.NeighborRouter;
import com.iota.iri.network.TransactionRequester;
import com.iota.iri.network.pipeline.TransactionProcessingPipeline;
import com.iota.iri.TransactionExecutor;
import com.iota.iri.utils.Converter;
import com.iota.iri.utils.Pair;
import com.iota.iri.vm.EVMTransaction;
import com.iota.iri.utils.IotaUtils;

import java.math.BigInteger;
import java.util.*;

// import org.apache.maven.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Strings;
import javax.xml.bind.DatatypeConverter;



/**
 * <p>
 * Creates a service instance that allows us to perform ledger state specific operations.
 * </p>
 * <p>
 * This class is stateless and does not hold any domain specific models.
 * </p>
 */
public class LedgerServiceImpl implements LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerServiceImpl.class);

    /**
     * Holds the tangle object which acts as a database interface.
     */
    private final Tangle tangle;

    /**
     * Holds the snapshot provider which gives us access to the relevant snapshots.
     */
    private final SnapshotProvider snapshotProvider;

    /**
     * Holds a reference to the service instance containing the business logic of the snapshot package.
     */
    private final SnapshotService snapshotService;

    /**
     * Holds a reference to the service instance containing the business logic of the milestone package.
     */
    private final MilestoneService milestoneService;

    private final SpentAddressesService spentAddressesService;

    private final BundleValidator bundleValidator;

    private Repository repository;

    private ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
    private CommonConfig commonConfig;

    /**
     * @param tangle Tangle object which acts as a database interface
     * @param snapshotProvider snapshot provider which gives us access to the relevant snapshots
     * @param snapshotService service instance of the snapshot package that gives us access to packages' business logic
     * @param milestoneService contains the important business logic when dealing with milestones
     */
    public LedgerServiceImpl(Tangle tangle, SnapshotProvider snapshotProvider, SnapshotService snapshotService,
            MilestoneService milestoneService, SpentAddressesService spentAddressesService,
                                  BundleValidator bundleValidator) {
        this.tangle = tangle;
        this.snapshotProvider = snapshotProvider;
        this.snapshotService = snapshotService;
        this.milestoneService = milestoneService;
        this.spentAddressesService = spentAddressesService;
        this.bundleValidator = bundleValidator;
    }

    @Override
    public void restoreLedgerState() throws LedgerException {
        try {
            Optional<MilestoneViewModel> milestone = milestoneService.findLatestProcessedSolidMilestoneInDatabase();
            if (milestone.isPresent()) {
                snapshotService.replayMilestones(snapshotProvider.getLatestSnapshot(), milestone.get().index());
            }
        } catch (Exception e) {
            throw new LedgerException("unexpected error while restoring the ledger state", e);
        }
    }

    @Override
    public boolean applyMilestoneToLedger(MilestoneViewModel milestone) throws LedgerException {
        log.debug("applying milestone {}", milestone.index());
        if(generateStateDiff(milestone)) {
            try {
                snapshotService.replayMilestones(snapshotProvider.getLatestSnapshot(), milestone.index());
            } catch (SnapshotException e) {
                throw new LedgerException("failed to apply the balance changes to the ledger state", e);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean tipsConsistent(List<Hash> tips) throws LedgerException {
        Set<Hash> visitedHashes = new HashSet<>();
        Map<Hash, Long> diff = new HashMap<>();
        for (Hash tip : tips) {
            if (!isBalanceDiffConsistent(visitedHashes, diff, tip)) {
                return false;
            }
        }

        return true;
    }
    public boolean isTransactionConfirmed(TransactionViewModel tvm){
        return milestoneService.isTransactionConfirmed(tvm);
    }

    @Override
    public boolean isBalanceDiffConsistent(Set<Hash> approvedHashes, Map<Hash, Long> diff, Hash tip) throws
            LedgerException {

        try {
            if (!TransactionViewModel.fromHash(tangle, tip).isSolid()) {
                return false;
            }
        } catch (Exception e) {
            throw new LedgerException("failed to check the consistency of the balance changes", e);
        }

        if (approvedHashes.contains(tip)) {
            return true;
        }
        Set<Hash> visitedHashes = new HashSet<>(approvedHashes);
        Map<Hash, Long> currentState = generateBalanceDiff(visitedHashes, tip,
                snapshotProvider.getLatestSnapshot().getIndex());
        if (currentState == null) {
            return false;
        }
        diff.forEach((key, value) -> {
            if (currentState.computeIfPresent(key, ((hash, aLong) -> value + aLong)) == null) {
                currentState.putIfAbsent(key, value);
            }
        });
        boolean isConsistent = snapshotProvider.getLatestSnapshot().patchedState(new SnapshotStateDiffImpl(
                currentState)).isConsistent();
        if (isConsistent) {
            diff.putAll(currentState);
            approvedHashes.addAll(visitedHashes);
        }
        return isConsistent;
    }

    @Override
    public Map<Hash, Long> generateBalanceDiff(Set<Hash> visitedTransactions, Hash startTransaction, int milestoneIndex)
            throws LedgerException {

        Map<Hash, Long> state = new HashMap<>();

        Snapshot initialSnapshot = snapshotProvider.getInitialSnapshot();
        Map<Hash, Integer> solidEntryPoints = initialSnapshot.getSolidEntryPoints();
        solidEntryPoints.keySet().forEach(solidEntryPointHash -> {
            visitedTransactions.add(solidEntryPointHash);
        });

        final Queue<Hash> nonAnalyzedTransactions = new LinkedList<>(Collections.singleton(startTransaction));
        Hash transactionPointer;
        while ((transactionPointer = nonAnalyzedTransactions.poll()) != null) {
            try {
                final TransactionViewModel transactionViewModel = TransactionViewModel.fromHash(tangle,
                        transactionPointer);
                if (transactionViewModel.getCurrentIndex() == 0 && visitedTransactions.add(transactionPointer)) {
                    if (transactionViewModel.getType() == TransactionViewModel.PREFILLED_SLOT) {
                        return null;
                    }
                    if (!milestoneService.isTransactionConfirmed(transactionViewModel, milestoneIndex)) {

                        final List<TransactionViewModel> bundleTransactions = bundleValidator.validate(tangle,
                                snapshotProvider.getInitialSnapshot(), transactionViewModel.getHash());

                        if (bundleTransactions.isEmpty()) {
                            return null;
                        }

                        // ISSUE 1008: generateBalanceDiff should be refactored so we don't have those hidden
                        // concerns
                        spentAddressesService.persistValidatedSpentAddressesAsync(bundleTransactions);

                        if (BundleValidator.isInconsistent(bundleTransactions)) {
                            log.error("Encountered an inconsistent bundle with tail {} and bundle hash {}",
                                    bundleTransactions.get(0).getHash(), bundleTransactions.get(0).getBundleHash());
                            return null;
                        }

                        for (final TransactionViewModel bundleTransactionViewModel : bundleTransactions) {
                            if (bundleTransactionViewModel.value() != 0) {

                                final Hash address = bundleTransactionViewModel.getAddressHash();
                                final Long value = state.get(address);
                                state.put(address, value == null ? bundleTransactionViewModel.value()
                                        : Math.addExact(value, bundleTransactionViewModel.value()));
                            }
                        }
                        nonAnalyzedTransactions.addAll(DAGHelper.get(tangle).findTails(transactionViewModel));
                    }

                }

            } catch (Exception e) {
                throw new LedgerException("unexpected error while generating the balance diff", e);
            }
        }

        return state;
    }


    /**
     * <p>
     * Generates the {@link com.iota.iri.model.StateDiff} that belongs to the given milestone in the database and marks
     * all transactions that have been approved by the milestone accordingly by setting their {@code snapshotIndex}
     * value.
     * </p>
     * <p>
     * It first checks if the {@code snapshotIndex} of the transaction belonging to the milestone was correctly set
     * already (to determine if this milestone was processed already) and proceeds to generate the {@link
     * com.iota.iri.model.StateDiff} if that is not the case. To do so, it calculates the balance changes, checks if
     * they are consistent and only then writes them to the database.
     * </p>
     * <p>
     * If inconsistencies in the {@code snapshotIndex} are found it issues a reset of the corresponding milestone to
     * recover from this problem.
     * </p>
     *
     * @param milestone the milestone that shall have its {@link com.iota.iri.model.StateDiff} generated
     * @return {@code true} if the {@link com.iota.iri.model.StateDiff} could be generated and {@code false} otherwise
     * @throws LedgerException if anything unexpected happens while generating the {@link com.iota.iri.model.StateDiff}
     */
    private boolean generateStateDiff(MilestoneViewModel milestone) throws LedgerException {
        try {
            TransactionViewModel transactionViewModel = TransactionViewModel.fromHash(tangle, milestone.getHash());

            if (!transactionViewModel.isSolid()) {
                return false;
            }
            repository = tangle.getRepository();
            final int transactionSnapshotIndex = transactionViewModel.snapshotIndex();
            boolean successfullyProcessed = transactionSnapshotIndex == milestone.index();
            if (!successfullyProcessed) {
                // if the snapshotIndex of our transaction was set already, we have processed our milestones in
                // the wrong order (i.e. while rescanning the db)
                if (transactionSnapshotIndex != 0) {
                    milestoneService.resetCorruptedMilestone(milestone.index());
                }

                snapshotProvider.getLatestSnapshot().lockRead();
                try {
                    Hash tail = transactionViewModel.getHash();
                    byte[] trits = transactionViewModel.trits();
                    TransactionViewModel trunk = transactionViewModel.getTrunkTransaction(tangle), branch = transactionViewModel.getBranchTransaction(tangle);
                    Map<Hash, Long> balanceChanges = generateBalanceDiff(new HashSet<>(), tail,
                            snapshotProvider.getLatestSnapshot().getIndex());
                    // System.out.println("<===========>Milestone Tags:"+ Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.TAG_TRINARY_OFFSET,TransactionViewModel.TAG_TRINARY_OFFSET+TransactionViewModel.TAG_TRINARY_SIZE)));

                    while(true){
                        if(!trunk.getBundleHash().equals(transactionViewModel.getBundleHash())){
                            break;
                        }
                        trits = trunk.trits();
                        // System.out.println("<===========>Milestone Tags:"+ Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.TAG_TRINARY_OFFSET,TransactionViewModel.TAG_TRINARY_OFFSET+TransactionViewModel.TAG_TRINARY_SIZE)));

                        branch = trunk.getBranchTransaction(tangle);
                        trunk = trunk.getTrunkTransaction(tangle);
                    }
                    Repository track = tangle.getRepository();
                    String milestoneSummary = Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.TAG_TRINARY_OFFSET,TransactionViewModel.TAG_TRINARY_OFFSET+TransactionViewModel.TAG_TRINARY_SIZE));
                    String tSummary = "";
                    String bSummary = "";
                    // log.info("Getting summaries {}",getLineInfo());
                    if(trunk.getHash().equals(branch.getHash())){
                        List<String> trunkSummary = getSummary(trunk.getHash(),track);
                        tSummary = "";
                        if(!trunkSummary.isEmpty()){
                            for(String tips0 : trunkSummary){
                                tSummary += Integer.toString(tips0.hashCode());
                            }
                        }
                        bSummary = tSummary;

                    }else{
                        List<String> trunkSummary = getSummary(trunk.getHash(),track);
                        List<String> branchSummary = getSummary(branch.getHash(),track);
                        if(!trunkSummary.isEmpty()){
                        for(String tips0 : trunkSummary){
                            tSummary += Integer.toString(tips0.hashCode());
                        }
                        }
                        if(!branchSummary.isEmpty()){
                        for(String tips1 :branchSummary){
                            bSummary += Integer.toString(tips1.hashCode());
                        }
                        }
                    }
                    // System.out.println("LocalTrunk&Branch=======>t:"+trunk.getHash().toString()+" b:"+branch.getHash().toString());

                    // System.out.println("LocalSummaries=======>t:"+tSummary+" b:"+bSummary);
                    String summaryHash =Integer.toString((Integer.toString(tSummary.hashCode())+Integer.toString(bSummary.hashCode())).hashCode());
                    String finalSummary = Strings.padEnd(Converter.asciiToTrytes(summaryHash),27,'9');
                    // String milestoneSummary = Converter.trytes(trits).substring(TransactionViewModel.TAG_TRINARY_OFFSET,TransactionViewModel.TAG_TRINARY_OFFSET+TransactionViewModel.TAG_TRINARY_SIZE);
                    if(!finalSummary.equals(milestoneSummary)){
                        // System.out.println("!!!!!!!!!!!Summaries not match!!!!!!!!!!!!");
                        // System.out.println("State change differs from milestone's: local=>" + finalSummary +" vs milestone=> "+ milestoneSummary);
                        // track.rollback();
                        // throw new Exception("State change differs from milestone's: local=>" + finalSummary +" vs milestone=> "+ milestoneSummary);
                    }else{
                        // System.out.println("~~~~~~~~~~~~~Summaries matched~~~~~~~~~~~~~~");
                    }
                    successfullyProcessed = balanceChanges != null;
                    if (successfullyProcessed) {
                        successfullyProcessed = snapshotProvider.getLatestSnapshot().patchedState(
                                new SnapshotStateDiffImpl(balanceChanges)).isConsistent();
                        if (successfullyProcessed) {
                            track.commit();
                            track.flush();
                            // log.info("[Contract] contract db commited");
                            milestoneService.updateMilestoneIndexOfMilestoneTransactions(milestone.getHash(),
                                    milestone.index());

                            if (!balanceChanges.isEmpty()) {
                                new StateDiffViewModel(balanceChanges, milestone.getHash()).store(tangle);
                            }
                        }
                        else{
                            track.rollback();
                        }
                    }
                } finally {
                    snapshotProvider.getLatestSnapshot().unlockRead();
                }
            }

            return successfullyProcessed;
        } catch (Exception e) {
            throw new LedgerException("unexpected error while generating the StateDiff for " + milestone, e);
        }
    
}


private Repository getRepository(){
    return repository;
}
private List<String> getSummary(Hash hash, Repository track){
    // log.info("Getting summary");

    List<String> summaries = new LinkedList<String>();
    String message = "";
    List<TransactionViewModel> bundleTransactions;
    try{
        bundleTransactions = bundleValidator.validate(tangle,
                snapshotProvider.getInitialSnapshot(), hash);
    }catch(Exception e){
        e.printStackTrace();
        return summaries;
    }
    if(bundleTransactions.isEmpty()){
        return summaries;
    }
    TransactionViewModel current = bundleTransactions.get(bundleTransactions.size()-1);
    TransactionViewModel trunk,branch;
    try{
     trunk = TransactionViewModel.fromHash(tangle, current.getTrunkTransactionHash());
    }catch(Exception e){
        e.printStackTrace();
        return summaries;
    }
    try{
        branch = TransactionViewModel.fromHash(tangle, current.getBranchTransactionHash());
    }catch(Exception e){
        e.printStackTrace();
        return summaries;
    }
    if(!isTransactionConfirmed(trunk)){
        summaries.addAll(getSummary(trunk.getHash(),track));
    }
    if(!isTransactionConfirmed(branch)){
        summaries.addAll(getSummary(branch.getHash(),track));
    }
    for(int i = 0; i < bundleTransactions.size(); i++){
        TransactionViewModel tvm = bundleTransactions.get(i);
        Transaction tx = tvm.getTransaction();
        // String trytes = Converter.trytes(tvm.trits());
        // String tag = trytes.substring(TransactionViewModel.TAG_TRINARY_OFFSET,TransactionViewModel.TAG_TRINARY_OFFSET+TransactionViewModel.TAG_TRINARY_SIZE);
        byte[] trits = tvm.trits();
        String tag = Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.TAG_TRINARY_OFFSET,TransactionViewModel.TAG_TRINARY_OFFSET+TransactionViewModel.TAG_TRINARY_SIZE));

        if(tag.startsWith("SMARTCONTRACT")){
            // log.info("preparing smart contract");

            byte[] toAddr = Converter.trytesToBytes(Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET, TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET + 120)));
            // log.info("To Address: "+DatatypeConverter.printHexBinary(toAddr));

            // String targetSmartContractAddress = tag.substring(13,19);
            String IOTAAddr =  Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.ADDRESS_TRINARY_OFFSET, TransactionViewModel.ADDRESS_TRINARY_OFFSET+TransactionViewModel.ADDRESS_TRINARY_SIZE));
            // log.info("IOTAAddr: "+IOTAAddr);

            AccountMap am = track.getAccountMap(Converter.trytesToBytes(IOTAAddr));
            // log.info("AccountMap got?"+(am != null));

            byte[] fromAddr = null;
            if(am != null){
            fromAddr = am.getEVMAddr();
            }
            if(fromAddr == null){
                fromAddr = DatatypeConverter.parseHexBinary("0000000000000000000000000000000000000000");
            }
            // log.info("From Address: "+DatatypeConverter.printHexBinary(fromAddr));

            message += Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET + 120,TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET + TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_SIZE));//get first message fragment
            try{
            while(i < bundleTransactions.size()){
                if(i+1 == bundleTransactions.size()){
                    break;
                }
                TransactionViewModel nextTx = bundleTransactions.get(i+1);
                trits = nextTx.trits();
                String nextTag = Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.TAG_TRINARY_OFFSET,TransactionViewModel.TAG_TRINARY_OFFSET+TransactionViewModel.TAG_TRINARY_SIZE));
                if(!nextTag.startsWith("SMARTCONTRACT")){
                    break;
                }
                else{
                    message += Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET,TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET + TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_SIZE));//get first message fragment
                    i++;
                }
            }}finally{
                // log.info("Got bytecode: "+ DatatypeConverter.printHexBinary(Converter.trytesToBytes(message)));
                // System.out.println("=============LedgerService getSummary=========");
                // System.out.println(DatatypeConverter.printHexBinary(fromAddr));
                // System.out.println(DatatypeConverter.printHexBinary(toAddr));
                // System.out.println(message.hashCode());
                EVMTransaction evmTx = ConvertToEVMTx(fromAddr, toAddr, message);//记得拿sender的evm address
                // log.info("Tx converted");

                // Repository txTrack = track.startTracking();
                // TransactionExecutor executor = new TransactionExecutor(evmTx, txTrack, programInvokeFactory).withCommonConfig(commonConfig.getDefault()).setLocalCall(true);
                TransactionExecutor executor = new TransactionExecutor(evmTx, track, programInvokeFactory).withCommonConfig(commonConfig.getDefault()).setLocalCall(true);
                // log.info("Executing smart contract");

                executor.init();
                // log.info("Executor inited");
                executor.execute();
 
                // log.info("Executor execute");
                executor.go();
                // log.info("Executor go");
                if(evmTx.getContractAddress()!=null){
                log.info("Smart contract 0x"+ DatatypeConverter.printHexBinary(evmTx.getContractAddress()));
                }
                // txTrack.commit();
                TransactionExecutionSummary summary = executor.finalization();
                // System.out.println("LocalSummary<=============="+summary.getEncoded().hashCode()+"");
                // summaries.add(summary.getEncoded().hashCode()+"");
                summaries.add(summary.getStorageChangeHash());
                message = "";
            }
        }
        if(tag.startsWith("ALIAS")){
            // Repository aliasTrack = track.startTracking();
            String IOTAAddr =  Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.ADDRESS_TRINARY_OFFSET, TransactionViewModel.ADDRESS_TRINARY_OFFSET+TransactionViewModel.ADDRESS_TRINARY_SIZE));
            byte[] toAddr = Converter.trytesToBytes(Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET, TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET + 120)));
            // log.info("=========================toAddr:"+ DatatypeConverter.printHexBinary(toAddr)+"===================");
            byte[] newAddr = Converter.trytesToBytes(Converter.trytes(Arrays.copyOfRange(trits,TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET+ 120, TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET + 120 + TransactionViewModel.ADDRESS_TRINARY_SIZE)));
            // log.info("Aliasing "+IOTAAddr+" to "+DatatypeConverter.printHexBinary(toAddr));
            byte[] oldAddr = Converter.trytesToBytes(IOTAAddr);
             if(track.getAccountState(toAddr) == null){
                track.createAccount(toAddr);
            }
            track.aliasAccount(oldAddr, toAddr, newAddr);
            // if(aliasTrack.getAccountState(toAddr) == null){
            //     aliasTrack.createAccount(toAddr);
            // }
            // aliasTrack.aliasAccount(oldAddr, toAddr, newAddr);
            // aliasTrack.commit();
            //checkyixia
            
        }


    }
    // for(String s:summaries){
    //     System.out.println(s);
    // }
    // track.commit();
    return summaries;
}

private EVMTransaction ConvertToEVMTx(byte[] fromAddr, byte[] toAddr, String data){
    // System.out.println("==========Converting Tx==========");
    byte[] value = new byte[1];
    value[0] = (byte) 0;
    byte[] nonce = null;
    BigInteger aaa = getRepository().getNonce(fromAddr);
    if(aaa!=null){
        nonce = (aaa.add(BigInteger.ONE)).toByteArray();
    }
    if(nonce == null){
    nonce = new byte[1];
    nonce[0] = (byte) 0;
    }
    // System.out.println("data"+data);
    // System.out.println("data without 0:"+data.replaceAll("9*$", ""));
    data = data.replaceAll("9*$","");
    // System.out.println("data"+data.hashCode());

    byte[] dataInBytes = Converter.trytesToBytes(data);
    // System.out.println("dataInBytes"+DatatypeConverter.printHexBinary(dataInBytes));
    EVMTransaction ret = new EVMTransaction(nonce, fromAddr, toAddr , value, dataInBytes);
    return ret;
}
public static String getLineInfo(){
    StackTraceElement ste = new Throwable().getStackTrace()[1];
    return ste.getFileName() + ": Line " + ste.getLineNumber();
}
}