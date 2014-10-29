package se.rosenbaum.bitcoiniblt.util;

import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.StopProcessingException;

public interface TransactionProcessor {
    public void process(Transaction transaction) throws StopProcessingException;
}
