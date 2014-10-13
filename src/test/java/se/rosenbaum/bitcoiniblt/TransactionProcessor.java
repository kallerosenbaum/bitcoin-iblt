package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Transaction;

public interface TransactionProcessor {
    public void process(Transaction transaction);
}
