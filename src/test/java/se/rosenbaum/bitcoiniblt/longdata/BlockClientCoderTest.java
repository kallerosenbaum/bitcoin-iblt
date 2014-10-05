package se.rosenbaum.bitcoiniblt.longdata;

import com.google.bitcoin.core.Block;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.ClientCoderTest;
import se.rosenbaum.bitcoiniblt.SameAsBlockTransactionSorter;
import se.rosenbaum.bitcoiniblt.longdata.BlockCoder;
import se.rosenbaum.bitcoiniblt.longdata.LongDataTransactionCoder;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;

import static org.junit.Assert.assertEquals;

public class BlockClientCoderTest extends ClientCoderTest {

    @Test
    public void testSimple() throws Exception {
        BlockCoder sut = new BlockCoder(10, 2, new LongDataTransactionCoder(params, new byte[32]), new SameAsBlockTransactionSorter(block));
        IBLT<LongData, LongData> iblt = sut.encode(block);
        Block result = sut.decode(block.cloneAsHeader(), iblt, block.getTransactions());
        assertEquals(block, result);
    }

}
