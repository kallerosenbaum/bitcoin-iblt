package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CanonicalOrderTransactionSorter implements TransactionSorter, Comparator<Transaction> {

    private class SortKey {
        BigInteger hash = null;
        long index = -1;
    }

    @Override
    public int compare(Transaction transaction, Transaction transaction2) {
        if (transaction == null || transaction2 == null) {
            throw new RuntimeException("Trying to compare a null transaction!");
        }
        if (transaction == transaction2) {
            return 0;
        }
        SortKey key = sortKey(transaction);
        SortKey key2 = sortKey(transaction2);
        int comparison = key.hash.compareTo(key2.hash);
        // negative if transaction is less than transaction2
        if (comparison == 0 && key.index != key2.index) {
            return key.index-key2.index < 0L ? -1 : 1;
        }
        return comparison;
    }

    SortKey sortKey(Transaction transaction) {
        SortKey sortKey = new SortKey();
        List<TransactionInput> inputs = transaction.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            TransactionOutPoint outpoint = inputs.get(i).getOutpoint();
            BigInteger hash = outpoint.getHash().toBigInteger();
            if (sortKey.hash == null) {
                sortKey.hash = hash;
                sortKey.index = outpoint.getIndex();
            } else if (sortKey.hash.compareTo(hash) > 0) {
                sortKey.hash = hash;
                sortKey.index = outpoint.getIndex();
            }
        }
        if (sortKey.hash == null) {
            throw new RuntimeException("No inputs found for transaction " + transaction.getHash() + " Cant create sort key");
        }
        return sortKey;
    }


    public List<Transaction> sort(List<Transaction> list) {
        ArrayList<Transaction> sortedList = new ArrayList<Transaction>(list);
        Collections.sort(sortedList, this);
        return sortedList;
    }

}
