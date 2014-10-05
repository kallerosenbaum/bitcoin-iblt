package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Transaction;

import java.util.List;

public interface TransactionSorter {
    public List<Transaction> sort(List<Transaction> list);
}
