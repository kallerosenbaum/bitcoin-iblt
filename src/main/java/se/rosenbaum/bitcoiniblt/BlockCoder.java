package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;

import java.math.BigInteger;
import java.util.*;

public class BlockCoder {
    private int cellCount;
    private int hashFunctionCount;

    private List<Transaction> sort(List<Transaction> list) {
        ArrayList<Transaction> sortedList = new ArrayList<Transaction>(list);
        Collections.sort(sortedList, new TransactionComparator());
        return sortedList;
    }


    public BlockCoder(int cellCount, int hashFunctionCount) {
        this.cellCount = cellCount;
        this.hashFunctionCount = hashFunctionCount;
    }

    public IBLT<LongData, LongData> encode(Block block) {
        IBLT<LongData, LongData> iblt = IBLTUtils.createIblt(cellCount, hashFunctionCount);
        List<Transaction> sorted = block.getTransactions();
        while (!sorted.isEmpty()) {
           // for (int i = 0; i < )
        }
        return iblt;
    }

    Iterator<Transaction> iterator(List<Transaction> list) {
        List<Transaction> sideList = new ArrayList<Transaction>();

        for (int i = 0; i < list.size(); i++) {
            Transaction transaction = list.get(i);
            for (int j = i + 1; j < list.size(); j++) {
                if (dependsOn(transaction, list.get(j))) {
                    sideList.add(transaction);
                }
            }
        }
    }

    private class TransactionIterator implements Iterator<Transaction> {
        List<Transaction> sideList = new ArrayList<Transaction>();
        List<Transaction> list;
        private int index = 0;

        private TransactionIterator(List<Transaction> list) {
            this.list = list;
        }

        @Override
        public boolean hasNext() {
            return !sideList.isEmpty() || index >= list.size();
        }

        @Override
        public Transaction next() {
            Transaction transaction;

            boolean found = false;
            while (!found) {
                if (!sideList.isEmpty()) {
                    for (int i = 1; i < sideList.size(); i++) {

                    }
                }
            }
            transaction = list.get(index);
            for (int j = i + 1; j < list.size(); j++) {
                if (dependsOn(transaction, list.get(j))) {
                    sideList.add(transaction);
                }
            }
            return false;  //To change body of implemented methods use File | Settings | File Templates.
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

    public Block decode(IBLT<LongData, LongData> iblt, List<Transaction> myTransactions) {
        return null;
    }
}
