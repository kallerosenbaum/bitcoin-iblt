package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CanonicalOrderTransactionSorter implements TransactionSorter, Comparator<Transaction> {

    private class SortKey {
        BigInteger hash = null;
        int index = -1;
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
        if (comparison == 0) {
            return key.index-key2.index;
        }
        return comparison;
    }

    private SortKey sortKey(Transaction transaction) {
        SortKey sortKey = new SortKey();
        List<TransactionInput> inputs = transaction.getInputs();
        for (int index = 0; index < inputs.size(); index++) {
            BigInteger hash = inputs.get(index).getHash().toBigInteger();
            if (sortKey.hash == null) {
                sortKey.hash = hash;
                sortKey.index = index;
            } else if (sortKey.hash.compareTo(hash) > 0) {
                sortKey.hash = hash;
                sortKey.index = index;
            }
        }
        return sortKey;
    }


    public List<Transaction> sort(List<Transaction> list) {
        ArrayList<Transaction> sortedList = new ArrayList<Transaction>(list);
        Collections.sort(sortedList, this);
        return sortedList;
    }

}
