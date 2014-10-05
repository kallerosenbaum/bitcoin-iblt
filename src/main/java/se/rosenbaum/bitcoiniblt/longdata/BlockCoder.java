package se.rosenbaum.bitcoiniblt.longdata;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import se.rosenbaum.bitcoiniblt.TransactionSorter;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;
import se.rosenbaum.iblt.util.ResidualData;

import java.util.*;

public class BlockCoder {
    private int cellCount;
    private int hashFunctionCount;
    private LongDataTransactionCoder transactionCoder;
    private TransactionSorter sorter;


    public BlockCoder(int cellCount, int hashFunctionCount, LongDataTransactionCoder transactionCoder, TransactionSorter sorter) {
        this.cellCount = cellCount;
        this.hashFunctionCount = hashFunctionCount;
        this.transactionCoder = transactionCoder;
        this.sorter = sorter;
    }

    public IBLT<LongData, LongData> encode(Block block) {
        IBLT<LongData, LongData> iblt = IBLTUtils.createIblt(cellCount, hashFunctionCount);
        List<Transaction> transactions = block.getTransactions();
        for (Transaction transaction : transactions) {
            Map<LongData, LongData> data = transactionCoder.encodeTransaction(transaction);
            for (Map.Entry<LongData, LongData> entry : data.entrySet()) {
                iblt.insert(entry.getKey(), entry.getValue());
            }
        }
        return iblt;
    }

    private class TransactionIterator implements Iterator<Transaction> {
        List<Transaction> sideList = new ArrayList<Transaction>();
        List<Transaction> sortedList;
        private int index = 0;

        private TransactionIterator(List<Transaction> sortedList) {
            this.sortedList = sortedList;
        }

        @Override
        public boolean hasNext() {
            return !sortedList.isEmpty();
        }

        @Override
        public Transaction next() {
            for (int i = 0; i < sortedList.size(); i++) {
                Transaction transaction = sortedList.get(i);
                boolean isDependee = false;
                for (int j = i + 1; j < sortedList.size(); j++) {
                    if (dependsOn(transaction, sortedList.get(j))) {
                        isDependee = true;
                        break;
                    }
                }
                if (!isDependee) {
                    return transaction;
                }
            }
            throw new RuntimeException("Either circular referencing transactions, or No transactions left");
        }

        @Override
        public void remove() {
        }
    }

    boolean dependsOn(Transaction dependant, Transaction dependee) {
        Sha256Hash dependeeHash = dependee.getHash();
        for (TransactionInput input : dependant.getInputs()) {
            if (input.getHash().equals(dependeeHash)) {
                return true;
            }
        }
        return false;
    }

    public Block decode(Block header, IBLT<LongData, LongData> iblt, List<Transaction> myTransactions) {
        List<Transaction> mutableList = new ArrayList<Transaction>();
        for (Transaction myTransaction : myTransactions) {
            Map<LongData, LongData> map = transactionCoder.encodeTransaction(myTransaction);
            for (Map.Entry<LongData, LongData> entry : map.entrySet()) {
                iblt.delete(entry.getKey(), entry.getValue());
            }
            mutableList.add(myTransaction);
        }
        ResidualData<LongData,LongData> residualData = iblt.listEntries();
        mutableList.removeAll(transactionCoder.decodeTransactions(residualData.getAbsentEntries()));
        mutableList.addAll(transactionCoder.decodeTransactions(residualData.getExtraEntries()));
        for (Transaction transaction : sorter.sort(mutableList)) {
            header.addTransaction(transaction);
        }
        return header;
    }

}
