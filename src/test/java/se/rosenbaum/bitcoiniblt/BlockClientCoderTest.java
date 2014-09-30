package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import org.junit.Test;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;

import static org.junit.Assert.assertEquals;

public class BlockClientCoderTest extends ClientCoderTest {

    @Test
    public void test() throws Exception {
        BlockCoder sut = new BlockCoder(10, 2);
        IBLT<LongData, LongData> iblt = sut.encode(block);
        Block result = sut.decode(iblt, block.getTransactions());
        assertEquals(block, result);
    }
}
