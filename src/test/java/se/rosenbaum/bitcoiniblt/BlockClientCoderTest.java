package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.ClientCoderTest;
import se.rosenbaum.bitcoiniblt.SameAsBlockTransactionSorter;
import se.rosenbaum.bitcoiniblt.bytearraydata.ByteArrayDataTransactionCoder;
import se.rosenbaum.bitcoiniblt.longdata.*;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;

import static org.junit.Assert.assertEquals;

public class BlockClientCoderTest extends ClientCoderTest {

    @Test
    public void testSimple() throws Exception {
        TransactionCoder transactionCoder = new ByteArrayDataTransactionCoder(params, new byte[32], 8, 32);
        SameAsBlockTransactionSorter sorter = new SameAsBlockTransactionSorter(block);
        BlockCoder sut = new BlockCoder(10, 2, transactionCoder, sorter);
        IBLT<LongData, LongData> iblt = sut.encode(block);
        Block result = sut.decode(block.cloneAsHeader(), iblt, block.getTransactions());
        assertEquals(block, result);
    }

}
