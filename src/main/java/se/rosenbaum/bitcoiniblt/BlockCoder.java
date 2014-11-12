package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.Data;
import se.rosenbaum.iblt.util.ResidualData;

import java.util.*;

public class BlockCoder<K extends Data, V extends Data> {
    private IBLT<K, V> iblt;
    private TransactionCoder transactionCoder;
    private TransactionSorter sorter;
    private int encodedEntriesCount = 0;
    private int residualEntriesCount = 0;

    public BlockCoder(IBLT<K, V> iblt, TransactionCoder transactionCoder, TransactionSorter sorter) {
        this.iblt = iblt;
        this.transactionCoder = transactionCoder;
        this.sorter = sorter;
    }

    public IBLT<K, V> encode(Block block) {
        List<Transaction> transactions = block.getTransactions();
        for (Transaction transaction : transactions) {
            Map<K, V> data = transactionCoder.encodeTransaction(transaction);
            encodedEntriesCount += data.size();
            for (Map.Entry<K, V> entry : data.entrySet()) {
                iblt.insert(entry.getKey(), entry.getValue());
            }
        }
        return iblt;
    }

    public Block decode(Block header, IBLT<K, V> iblt, List<Transaction> myTransactions) {
        List<Transaction> mutableList = new ArrayList<Transaction>();
        // Gavin suggests that we build our own IBLT and then
        // do the IBLT_new - IBLT_us operation. I don't see
        // why that's necessary. I just remove the transactions
        // from IBLT_new one by one instead. Removing a transaction
        // from IBLT_new is the same cost as adding it to IBLT_us.
      //  Map<K, Map<K, V>> keyPrefixToEncodedTransaction = new HashMap<K, Map<K, V>>();
        for (Transaction myTransaction : myTransactions) {
            Map<K, V> map = transactionCoder.encodeTransaction(myTransaction);
            K key;
            for (Map.Entry<K, V> entry : map.entrySet()) {
                iblt.delete(entry.getKey(), entry.getValue());
        ///        key.
            }
        //    keyPrefixToEncodedTransaction.put(, map);
            mutableList.add(myTransaction);
        }
        ResidualData<K, V> residualData = iblt.listEntries();
        if (residualData == null) {
            return null;
        }
        residualEntriesCount = residualData.getAbsentEntries().size() + residualData.getExtraEntries().size();
        Collection absentTransactions = transactionCoder.decodeTransactions(residualData.getAbsentEntries());
        Collection extraTransactions = transactionCoder.decodeTransactions(residualData.getExtraEntries());
        mutableList.removeAll(absentTransactions);
        mutableList.addAll(extraTransactions);
        List<Transaction> sortedTransactions = sorter.sort(mutableList);
        int i = 0;
        for (Transaction transaction : sortedTransactions) {
            header.addTransaction(transaction);
        }
        return header;
    }

    public int getEncodedEntriesCount() {
        return encodedEntriesCount;
    }

    public int getResidualEntriesCount() {
        return residualEntriesCount;
    }
}
