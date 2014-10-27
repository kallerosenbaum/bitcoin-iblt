package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Transaction;

public interface TransactionProcessor {
    public void process(Transaction transaction) throws StopProcessingException;
}
