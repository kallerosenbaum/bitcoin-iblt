package se.rosenbaum.bitcoiniblt.util;

import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.StopProcessingException;

import java.util.ArrayList;
import java.util.List;

/**
* User: kalle
* Date: 10/29/14 9:38 PM
*/
public class TransactionCollectorProcessor implements TransactionProcessor {
    private List<Transaction> transactions = new ArrayList<Transaction>();
    private int count;

    public TransactionCollectorProcessor(int count) {
        this.count = count;
    }

    @Override
    public void process(Transaction transaction) throws StopProcessingException {
        if (transaction.isCoinBase()) {
            if (getTransactions().size() == 0) {
                getTransactions().add(transaction);
            } else {
                return;
            }
        } else {
            getTransactions().add(transaction);
        }
        if (getTransactions().size() == count) {
            throw new StopProcessingException();
        }
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
