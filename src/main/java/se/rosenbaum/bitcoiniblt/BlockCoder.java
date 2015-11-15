package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.Data;

import java.util.ArrayList;
import java.util.List;

public class BlockCoder<K extends Data, V extends Data> {
    private TransactionSorter sorter;
    IBLTTransactionMap<K, V> transactionMap;
    private ResidualTransactions residualTransactions;

    public BlockCoder(IBLT<K, V> iblt, TransactionCoder transactionCoder, TransactionSorter sorter) {
        transactionMap = new IBLTTransactionMap<K, V>(iblt, transactionCoder);
        this.sorter = sorter;
    }

    public IBLT<K, V> encode(Block block) {
        List<Transaction> transactions = block.getTransactions();
        transactionMap.putTransactions(transactions);
        return transactionMap.getIBLT();
    }

    public Block decode(Block header, List<Transaction> myTransactions) {
        List<Transaction> mutableList = new ArrayList<Transaction>();
        // Gavin suggests that we build our own IBLT and then
        // do the IBLT_new - IBLT_us operation. I don't see
        // why that's necessary. I just remove the transactions
        // from IBLT_new one by one instead. Removing a transaction
        // from IBLT_new is the same cost as adding it to IBLT_us.
        transactionMap.deleteTransactions(myTransactions);
        mutableList.addAll(myTransactions);

        residualTransactions = transactionMap.decodeRemaining();
        if (residualTransactions == null) {
            return null;
        }
        mutableList.removeAll(residualTransactions.getAbsentTransactions());
        mutableList.addAll(residualTransactions.getExtraTransactions());
        List<Transaction> sortedTransactions = sorter.sort(mutableList);
        int i = 0;
        for (Transaction transaction : sortedTransactions) {
            header.addTransaction(transaction);
        }
        return header;
    }

    public int getEncodedEntriesCount() {
        return transactionMap.getEncodedEntriesCount();
    }

    public int getResidualEntriesCount() {
        return transactionMap.getResidualEntriesCount();
    }

    public ResidualTransactions getResidualTransactions() {
        return residualTransactions;
    }
}
