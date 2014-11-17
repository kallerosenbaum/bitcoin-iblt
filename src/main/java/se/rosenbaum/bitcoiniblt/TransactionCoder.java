package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Transaction;

import java.util.Collection;
import java.util.Map;

public interface TransactionCoder<K, V> {
    Map<K, V> encodeTransaction(Transaction transaction);

    Collection<Transaction> decodeTransactions(Map<K, V> entries);

    Map<K, Map<K, V>> getEncodedTransactions();
}
