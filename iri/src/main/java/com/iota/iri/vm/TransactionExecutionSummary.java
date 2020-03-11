/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.iota.iri.vm;

import com.iota.iri.vm.EVMTransaction ;
import com.iota.iri.vm.util.ByteUtil;
import com.iota.iri.vm.util.RLP;
import com.iota.iri.vm.util.RLPElement;
import com.iota.iri.vm.util.RLPList;
import com.iota.iri.vm.vm.DataWord;
import com.iota.iri.vm.vm.LogInfo;
import com.iota.iri.vm.program.InternalTransaction;
import org.springframework.util.Assert;
import com.iota.iri.vm.core.TransactionTouchedStorage;
import java.math.BigInteger;
import java.util.*;

import static java.util.Collections.*;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static com.iota.iri.vm.util.BIUtil.toBI;

public class TransactionExecutionSummary {

    private EVMTransaction tx;
    private BigInteger value = BigInteger.ZERO;
    private BigInteger gasPrice = BigInteger.ZERO;
    private BigInteger gasLimit = BigInteger.ZERO;
    private BigInteger gasUsed = BigInteger.ZERO;
    private BigInteger gasLeftover = BigInteger.ZERO;
    private BigInteger gasRefund = BigInteger.ZERO;

    private List<DataWord> deletedAccounts = emptyList();
    private List<InternalTransaction> internalTransactions = emptyList();
    private Map<DataWord, DataWord> storageDiff = emptyMap();
    private TransactionTouchedStorage touchedStorage = new TransactionTouchedStorage();
    private String storageChangeHash;

    private byte[] result;
    private List<LogInfo> logs;

    private boolean failed;

    private byte[] rlpEncoded;
    private boolean parsed;


    public TransactionExecutionSummary(EVMTransaction transaction) {
        this.tx = transaction;
        // this.gasLimit = toBI(transaction.getGasLimit());
        // this.gasPrice = toBI(transaction.getGasPrice());
        this.value = toBI(transaction.getValue());
    }

    public TransactionExecutionSummary(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
        this.parsed = false;
    }

    public void rlpParse() {
        if (parsed) return;

        RLPList decodedTxList = RLP.decode2(rlpEncoded);
        RLPList summary = (RLPList) decodedTxList.get(0);

        this.tx = new EVMTransaction(summary.get(0).getRLPData());
        this.value = decodeBigInteger(summary.get(1).getRLPData());
        // this.gasPrice = decodeBigInteger(summary.get(2).getRLPData());
        // this.gasLimit = decodeBigInteger(summary.get(3).getRLPData());
        // this.gasUsed = decodeBigInteger(summary.get(4).getRLPData());
        // this.gasLeftover = decodeBigInteger(summary.get(5).getRLPData());
        // this.gasRefund = decodeBigInteger(summary.get(6).getRLPData());
        this.deletedAccounts = decodeDeletedAccounts((RLPList) summary.get(7));
        this.internalTransactions = decodeInternalTransactions((RLPList) summary.get(8));
        this.touchedStorage = decodeTouchedStorage(summary.get(9));
        this.result = summary.get(10).getRLPData();
        this.logs = decodeLogs((RLPList) summary.get(11));
        byte[] failed = summary.get(12).getRLPData();
        this.failed = isNotEmpty(failed) && RLP.decodeInt(failed, 0) == 1;
    }

    private static BigInteger decodeBigInteger(byte[] encoded) {
        return ByteUtil.bytesToBigInteger(encoded);
    }

    public byte[] getEncoded() {
        if (rlpEncoded != null) return rlpEncoded;


        this.rlpEncoded = RLP.encodeList(
                this.tx.getEncoded(),
                RLP.encodeBigInteger(this.value),
                RLP.encodeBigInteger(BigInteger.ZERO),
                RLP.encodeBigInteger(BigInteger.ZERO),
                RLP.encodeBigInteger(BigInteger.ZERO),
                RLP.encodeBigInteger(BigInteger.ZERO),
                RLP.encodeBigInteger(BigInteger.ZERO),
                encodeDeletedAccounts(this.deletedAccounts),
                encodeInternalTransactions(this.internalTransactions),
                encodeTouchedStorage(this.touchedStorage),
                RLP.encodeElement(this.result),
                encodeLogs(this.logs),
                RLP.encodeInt(this.failed ? 1 : 0)
        );

        return rlpEncoded;
    }

    public static byte[] encodeTouchedStorage(TransactionTouchedStorage touchedStorage) {
        Collection<TransactionTouchedStorage.Entry> entries = touchedStorage.getEntries();
        byte[][] result = new byte[entries.size()][];

        int i = 0;
        for (TransactionTouchedStorage.Entry entry : entries) {
            byte[] key = RLP.encodeElement(entry.getKey().getData());
            byte[] value = RLP.encodeElement(entry.getValue().getData());
            byte[] changed = RLP.encodeInt(entry.isChanged() ? 1 : 0);

            result[i++] = RLP.encodeList(key, value, changed);
        }

        return RLP.encodeList(result);
    }

    public void setStorageChangeHash(String s){
        this.storageChangeHash = s;
    }

    public String getStorageChangeHash(){
        if(this.storageChangeHash == null){
            this.storageChangeHash = "";
        }
        return this.storageChangeHash;
    }

    public static TransactionTouchedStorage decodeTouchedStorage(RLPElement encoded) {
        TransactionTouchedStorage result = new TransactionTouchedStorage();

        for (RLPElement entry : (RLPList) encoded) {
            RLPList asList = (RLPList) entry;

            DataWord key = DataWord.of(asList.get(0).getRLPData());
            DataWord value = DataWord.of(asList.get(1).getRLPData());
            byte[] changedBytes = asList.get(2).getRLPData();
            boolean changed = isNotEmpty(changedBytes) && RLP.decodeInt(changedBytes, 0) == 1;

            result.add(new TransactionTouchedStorage.Entry(key, value, changed));
        }

        return result;
    }

    private static List<LogInfo> decodeLogs(RLPList logs) {
        ArrayList<LogInfo> result = new ArrayList<>();
        for (RLPElement log : logs) {
            result.add(new LogInfo(log.getRLPData()));
        }
        return result;
    }

    private static byte[] encodeLogs(List<LogInfo> logs) {
        byte[][] result = new byte[logs.size()][];
        for (int i = 0; i < logs.size(); i++) {
            LogInfo log = logs.get(i);
            result[i] = log.getEncoded();
        }

        return RLP.encodeList(result);
    }

    private static byte[] encodeStorageDiff(Map<DataWord, DataWord> storageDiff) {
        byte[][] result = new byte[storageDiff.size()][];
        int i = 0;
        for (Map.Entry<DataWord, DataWord> entry : storageDiff.entrySet()) {
            byte[] key = RLP.encodeElement(entry.getKey().getData());
            byte[] value = RLP.encodeElement(entry.getValue().getData());
            result[i++] = RLP.encodeList(key, value);
        }
        return RLP.encodeList(result);
    }

    private static Map<DataWord, DataWord> decodeStorageDiff(RLPList storageDiff) {
        Map<DataWord, DataWord> result = new HashMap<>();
        for (RLPElement entry : storageDiff) {
            DataWord key = DataWord.of(((RLPList) entry).get(0).getRLPData());
            DataWord value = DataWord.of(((RLPList) entry).get(1).getRLPData());
            result.put(key, value);
        }
        return result;
    }

    private static byte[] encodeInternalTransactions(List<InternalTransaction> internalTransactions) {
        byte[][] result = new byte[internalTransactions.size()][];
        for (int i = 0; i < internalTransactions.size(); i++) {
            InternalTransaction transaction = internalTransactions.get(i);
            result[i] = transaction.getEncoded();
        }

        return RLP.encodeList(result);
    }

    private static List<InternalTransaction> decodeInternalTransactions(RLPList internalTransactions) {
        List<InternalTransaction> result = new ArrayList<>();
        for (RLPElement internalTransaction : internalTransactions) {
            result.add(new InternalTransaction(internalTransaction.getRLPData()));
        }
        return result;
    }

    private static byte[] encodeDeletedAccounts(List<DataWord> deletedAccounts) {
        byte[][] result = new byte[deletedAccounts.size()][];
        for (int i = 0; i < deletedAccounts.size(); i++) {
            DataWord deletedAccount = deletedAccounts.get(i);
            result[i] = RLP.encodeElement(deletedAccount.getData());

        }
        return RLP.encodeList(result);
    }

    private static List<DataWord> decodeDeletedAccounts(RLPList deletedAccounts) {
        List<DataWord> result = new ArrayList<>();
        for (RLPElement deletedAccount : deletedAccounts) {
            result.add(DataWord.of(deletedAccount.getRLPData()));
        }
        return result;
    }

    public EVMTransaction getTransaction() {
        if (!parsed) rlpParse();
        return tx;
    }

    public byte[] getTransactionHash() {
        return getTransaction().getHash();
    }

    private BigInteger calcCost(BigInteger gas) {
        return BigInteger.ZERO;
    }

    public BigInteger getFee() {
        return calcCost(BigInteger.ZERO);
    }

    public BigInteger getRefund() {
        return calcCost(BigInteger.ZERO);
    }

    public BigInteger getLeftover() {
        return calcCost(BigInteger.ZERO);
    }

    public BigInteger getGasPrice() {
        return BigInteger.ZERO;
    }

    public BigInteger getGasLimit() {
        return BigInteger.ZERO;
    }

    public BigInteger getGasUsed() {
        return BigInteger.ZERO;
    }

    public BigInteger getGasLeftover() {
        return BigInteger.ZERO;
    }

    public BigInteger getValue() {
        if (!parsed) rlpParse();
        return value;
    }

    public List<DataWord> getDeletedAccounts() {
        if (!parsed) rlpParse();
        return deletedAccounts;
    }

    public List<InternalTransaction> getInternalTransactions() {
        if (!parsed) rlpParse();
        return internalTransactions;
    }

    @Deprecated
    /* Use getTouchedStorage().getAll() instead */
    public Map<DataWord, DataWord> getStorageDiff() {
        if (!parsed) rlpParse();
        return storageDiff;
    }

    public BigInteger getGasRefund() {
        return BigInteger.ZERO;
    }

    public boolean isFailed() {
        if (!parsed) rlpParse();
        return failed;
    }

    public byte[] getResult() {
        if (!parsed) rlpParse();
        return result;
    }

    public List<LogInfo> getLogs() {
        if (!parsed) rlpParse();
        return logs;
    }

    public TransactionTouchedStorage getTouchedStorage() {
        return touchedStorage;
    }

    public static Builder builderFor(EVMTransaction transaction) {
        return new Builder(transaction);
    }

    public static class Builder {

        private final TransactionExecutionSummary summary;

        Builder(EVMTransaction transaction) {
            Assert.notNull(transaction, "Cannot build TransactionExecutionSummary for null transaction.");
            summary = new TransactionExecutionSummary(transaction);
        }

        public Builder gasUsed(BigInteger gasUsed) {
            summary.gasUsed = gasUsed;
            return this;
        }

        public Builder gasLeftover(BigInteger gasLeftover) {
            summary.gasLeftover = gasLeftover;
            return this;
        }
        public Builder gasLeftover(int gasLeftover){
            BigInteger glf = BigInteger.valueOf(gasLeftover);
            summary.gasLeftover = glf;
            return this;
        }

        public Builder gasRefund(BigInteger gasRefund) {
            summary.gasRefund = gasRefund;
            return this;
        }

        public Builder internalTransactions(List<InternalTransaction> internalTransactions) {
            summary.internalTransactions = unmodifiableList(internalTransactions);
            return this;
        }

        public Builder deletedAccounts(Set<DataWord> deletedAccounts) {
            summary.deletedAccounts = new ArrayList<>();
            for (DataWord account : deletedAccounts) {
                summary.deletedAccounts.add(account);
            }
            return this;
        }

        public Builder storageDiff(Map<DataWord, DataWord> storageDiff) {
            summary.storageDiff = unmodifiableMap(storageDiff);
            return this;
        }

        public Builder touchedStorage(Map<DataWord, DataWord> touched, Map<DataWord, DataWord> changed) {
            summary.touchedStorage.addReading(touched);
            summary.touchedStorage.addWriting(changed);
            return this;
        }

        public Builder markAsFailed() {
            summary.failed = true;
            return this;
        }

        public Builder logs(List<LogInfo> logs) {
            summary.logs = logs;
            return this;
        }

        public Builder result(byte[] result) {
            summary.result = result;
            return this;
        }

        public TransactionExecutionSummary build() {
            summary.parsed = true;
            if (summary.failed) {
                for (InternalTransaction transaction : summary.internalTransactions) {
                    transaction.reject();
                }
            }
            return summary;
        }
    }
}
