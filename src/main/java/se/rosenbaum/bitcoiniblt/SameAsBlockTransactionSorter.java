package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Transaction;

import java.util.ArrayList;
import java.util.List;

public class SameAsBlockTransactionSorter implements TransactionSorter {
    Block blockToImitate;

    public SameAsBlockTransactionSorter(Block blockToImitate) {
        this.blockToImitate = blockToImitate;
    }

    @Override
    public List<Transaction> sort(List<Transaction> list) {
        List<Transaction> result = new ArrayList<Transaction>(list.size());
        for (Transaction transaction : blockToImitate.getTransactions()) {
            int index = list.indexOf(transaction);
            if (index !=  -1) {
                result.add(list.remove(index));
            }
        }
        return result;
    }
}
