package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

public interface TransactionStore {
    Transaction getTransaction(Sha256Hash hash) throws Exception;

}
