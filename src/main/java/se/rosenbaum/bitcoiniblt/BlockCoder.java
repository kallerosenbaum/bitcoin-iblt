package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Transaction;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.Data;
import se.rosenbaum.iblt.util.ResidualData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockCoder<K extends Data, V extends Data> {
    private IBLT<K, V> iblt;
    private TransactionCoder transactionCoder;
    private TransactionSorter sorter;

    public BlockCoder(IBLT<K, V> iblt, TransactionCoder transactionCoder, TransactionSorter sorter) {
        this.iblt = iblt;
        this.transactionCoder = transactionCoder;
        this.sorter = sorter;
    }

    public IBLT<K, V> encode(Block block) {
        List<Transaction> transactions = block.getTransactions();
        for (Transaction transaction : transactions) {
            Map<K, V> data = transactionCoder.encodeTransaction(transaction);
            for (Map.Entry<K, V> entry : data.entrySet()) {
                iblt.insert(entry.getKey(), entry.getValue());
            }
        }
        return iblt;
    }

    public Block decode(Block header, IBLT<K, V> iblt, List<Transaction> myTransactions) {
        List<Transaction> mutableList = new ArrayList<Transaction>();
        for (Transaction myTransaction : myTransactions) {
            Map<K, V> map = transactionCoder.encodeTransaction(myTransaction);
            for (Map.Entry<K, V> entry : map.entrySet()) {
                iblt.delete(entry.getKey(), entry.getValue());
            }
            mutableList.add(myTransaction);
        }
        ResidualData<K, V> residualData = iblt.listEntries();
        if (residualData == null) {
            return null;
        }
        mutableList.removeAll(transactionCoder.decodeTransactions(residualData.getAbsentEntries()));
        mutableList.addAll(transactionCoder.decodeTransactions(residualData.getExtraEntries()));
        for (Transaction transaction : sorter.sort(mutableList)) {
            header.addTransaction(transaction);
        }
        return header;
    }

}