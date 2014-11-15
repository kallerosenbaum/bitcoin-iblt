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
        ResidualData<K, V> residualData = iblt.listEntries();
        if (residualData == null) {
            return null;
        }
        residualEntriesCount = residualData.getAbsentEntries().size() + residualData.getExtraEntries().size();
        Collection<Transaction> extraTransactions = transactionCoder.decodeTransactions(residualData.getExtraEntries());
        Collection<Transaction> absentTransactions = transactionCoder.decodeTransactions(residualData.getAbsentEntries());
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
