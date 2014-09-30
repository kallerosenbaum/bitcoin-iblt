package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;

import java.math.BigInteger;
import java.util.*;

public class BlockCoder {
    private int cellCount;
    private int hashFunctionCount;

    private List<Transaction> sort(List<Transaction> list) {
        ArrayList<Transaction> sortedList = new ArrayList<Transaction>(list);
        Collections.sort(sortedList, new TransactionComparator());
        return sortedList;
    }


    public BlockCoder(int cellCount, int hashFunctionCount) {
        this.cellCount = cellCount;
        this.hashFunctionCount = hashFunctionCount;
    }

    public IBLT<LongData, LongData> encode(Block block) {
        IBLT<LongData, LongData> iblt = IBLTUtils.createIblt(cellCount, hashFunctionCount);
        List<Transaction> sorted = block.getTransactions();
        while (!sorted.isEmpty()) {
           // for (int i = 0; i < )
        }
        return iblt;
    }

    Iterator<Transaction> iterator(List<Transaction> list) {
                 return null;
    }

    boolean dependsOn(Transaction t1, Transaction t2) {
        return false;
    }

    public Block decode(IBLT<LongData, LongData> iblt, List<Transaction> myTransactions) {
        return null;
    }
}
