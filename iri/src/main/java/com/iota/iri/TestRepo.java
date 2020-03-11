import com.iota.iri.utils.Converter;
import com.iota.iri.controllers.TransactionViewModel;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.DatatypeConverter;
import javax.xml.crypto.Data;

import com.google.common.base.Strings;

import com.iota.iri.vm.datasource.rocksdb.RocksDbDataSource;
import com.iota.iri.vm.datasource.JournalSource;
import com.iota.iri.vm.Repository;
import com.iota.iri.vm.RepositoryRoot;
import com.iota.iri.vm.datasource.DbSettings;
import com.iota.iri.vm.datasource.DbSource;
import com.iota.iri.vm.vm.DataWord;
import com.iota.iri.TransactionExecutor;
import com.iota.iri.vm.EVMTransaction;
import com.iota.iri.vm.program.invoke.ProgramInvokeFactory;
import com.iota.iri.vm.program.invoke.ProgramInvokeFactoryImpl;
import com.iota.iri.vm.config.CommonConfig;

// import com.iota.iri.vm.datasource.leveldb.LevelDbDataSource;
// import com.iota.iri.vm.datasource.DbSource;
// import org.iota.jota.module.Transfer;

public class TestRepo{

    public static void main(String[] args) {
        byte[] root = createDB();
        getDB(root);
        // createDB();

        // TransactionExecutionSummary summary = executor.finalization();
    }

    private static void getDB(byte[] root){
        CommonConfig commonConfig = new CommonConfig();
        DbSource<byte[]> stateDb = new RocksDbDataSource("test");
        stateDb.init(DbSettings.DEFAULT);
        JournalSource<byte[]> pruningStateDS = new JournalSource<>(stateDb);
        RepositoryRoot track = new RepositoryRoot(pruningStateDS, root);
        byte[] contractAddr = DatatypeConverter.parseHexBinary("bd770416a3345f91e4b34576cb804a576fa48eb1");
        System.out.println(track.getStorageValue(contractAddr, DataWord.of(0)));
    }

    private static byte[] createDB() {
        ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
        CommonConfig commonConfig = new CommonConfig();

        DbSource<byte[]> stateDb = new RocksDbDataSource("test");
        stateDb.init(DbSettings.DEFAULT);
        JournalSource<byte[]> pruningStateDS = new JournalSource<>(stateDb);
        RepositoryRoot track = new RepositoryRoot(pruningStateDS);
        byte[] fromAddr = DatatypeConverter.parseHexBinary("0000000000000000000000000000000000000000");
        byte[] toAddr =  DatatypeConverter.parseHexBinary("0000000000000000000000000000000000000000");
        byte[] value = new byte[1];
        value[0] = (byte) 0;
        // byte[] nonce = getRepository().getAccountMap(fromAddr).getNonce().toByteArray();
        byte[] nonce = new byte[1];
        nonce[0] = (byte) 0;
        byte[] dataInBytes = DatatypeConverter.parseHexBinary("6080604052607b600055348015601457600080fd5b506096806100236000396000f300608060405260043610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff168063e8927fbc146044575b600080fd5b348015604f57600080fd5b5060566058565b005b600160008082825401925050819055505600a165627a7a72305820b39d0dcfbc776fc881b3e1ee483984405ee3210aa8b5219053e671c770d2f00c0029");
        EVMTransaction evmTx = new EVMTransaction(nonce, fromAddr, toAddr , value, dataInBytes);


        Repository txTrack = track.startTracking();
        TransactionExecutor executor = new TransactionExecutor(evmTx, txTrack, programInvokeFactory).withCommonConfig(commonConfig.getDefault()).setLocalCall(true);
        executor.init();
        System.out.println("==============Executor inited==============");
        try{
        executor.execute();
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("==============Executor executed==============");
        executor.go();
        System.out.println("==============Executor goed==============");
        if(evmTx.getContractAddress()!=null){
        // log.info("Smart contract 0x"+ DatatypeConverter.printHexBinary(evmTx.getContractAddress()));
        }
        txTrack.commit();
        byte[] root = track.getRoot();
        byte[] contractAddr = DatatypeConverter.parseHexBinary("bd770416a3345f91e4b34576cb804a576fa48eb1");
        System.out.println(track.getStorageValue(contractAddr, DataWord.of(0)));
        stateDb.close();
        return root;

    }
}