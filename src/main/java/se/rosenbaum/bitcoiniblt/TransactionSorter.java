package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Transaction;

import java.util.List;

public interface TransactionSorter {
    public List<Transaction> sort(List<Transaction> list);
}
