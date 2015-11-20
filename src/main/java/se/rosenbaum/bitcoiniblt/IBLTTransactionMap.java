package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Transaction;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.Data;
import se.rosenbaum.iblt.util.ResidualData;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class IBLTTransactionMap<K extends Data, V extends Data> {
    private IBLT<K, V> iblt;
    private TransactionCoder transactionCoder;
    private int residualEntriesCount = 0;
    private int encodedEntriesCount = 0;

    public IBLTTransactionMap(IBLT<K, V> iblt, TransactionCoder transactionCoder) {
        this.iblt = iblt;
        this.transactionCoder = transactionCoder;
    }

    public void putTransactions(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            Map<K, V> data = transactionCoder.encodeTransaction(transaction);
            encodedEntriesCount += data.size();
            for (Map.Entry<K, V> entry : data.entrySet()) {
                iblt.insert(entry.getKey(), entry.getValue());
            }
        }
    }

    public void deleteTransactions(List<Transaction> transactions) {
        for (Transaction myTransaction : transactions) {
            Map<K, V> map = transactionCoder.encodeTransaction(myTransaction);
            for (Map.Entry<K, V> entry : map.entrySet()) {
                iblt.delete(entry.getKey(), entry.getValue());
            }
        }
    }

    public ResidualTransactions decodeRemaining() {
        AbsentTxAddingListEntriesListener listener = null;
        Map encodedTransactions = transactionCoder.getEncodedTransactions();
        if (encodedTransactions != null) {
            listener = new AbsentTxAddingListEntriesListener(iblt, encodedTransactions);
        }
        ResidualData<K, V> residualData = iblt.listEntries(listener);
        if (residualData == null) {
            return null;
        }
        Map<K, V> extraEntries = residualData.getExtraEntries();
        residualEntriesCount = residualData.getAbsentEntries().size() + extraEntries.size();
        Collection<Transaction> extraTransactions = transactionCoder.decodeTransactions(extraEntries);

        if (extraTransactions == null) {
            // Couldn't assemble transactions due to failed listEntries.
            // This can happen if we have no or too small keyHashSum. listEntries
            // will simply believe all 1:s and -1:s are pure.
            return null;
        }
        for (Transaction extraTransaction : extraTransactions) {
            if (transactionCoder.isEncoded(extraTransaction)) {
                // This means that a guessed-transaction appears among the block-only transactions
                // This can happen if keyHashSum is of small size or zero size.
                // Suppose we add two slices from same tx, X, to a cell:
                // xxxxxx01 + xxxxxx02 = 00000003 (count=2)
                // Then we remove a slice from another tx, Y:
                // 00000003 + yyyyyy01 = yyyyyy02 (count=1)
                // This can now be read as a correct block-only slice. With a bit of bad luch
                // this can happen to all slices of Y and the tx would assemble just fine.
                return null;
            }
        }

        Collection<Transaction> absentTransactions;
        if (listener == null) {
            absentTransactions = transactionCoder.decodeTransactions(residualData.getAbsentEntries());
        } else {
            absentTransactions = transactionCoder.decodeTransactions(listener.getAbsentEntries());
        }
        if (absentTransactions == null) {
            // See comment in null check for extraTransactions above
            return null;
        }
        ResidualTransactions result = new ResidualTransactions(extraTransactions, absentTransactions);
        return result;
    }

    public IBLT<K, V> getIBLT() {
        return iblt;
    }

    public int getResidualEntriesCount() {
        return residualEntriesCount;
    }

    public int getEncodedEntriesCount() {
        return encodedEntriesCount;
    }
}
