package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Transaction;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;

import java.util.List;

public class BlockCoder {
    public IBLT<LongData, LongData> encode(Block block) {
        return null;
    }

    public Block decode(IBLT<LongData, LongData> iblt, List<Transaction> myTransactions) {
        return null;
    }
}
