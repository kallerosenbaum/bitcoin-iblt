package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Transaction;
import org.junit.Before;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.bytearraydata.ByteArrayDataTransactionCoder;
import se.rosenbaum.bitcoiniblt.bytearraydata.IBLTUtils;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BlockClientCoderTest extends ClientCoderTest {

    private TransactionCoder transactionCoder;
    private SameAsBlockTransactionSorter sorter;
    private BlockCoder sut;

    @Before
    public void setup() {
        transactionCoder = new ByteArrayDataTransactionCoder(getParams(), salt, 8, 32);
        sorter = new SameAsBlockTransactionSorter(getBlock());
        IBLT iblt = new IBLTUtils().createIblt(1600, 4, 8, 32, 4);
        sut = new BlockCoder(iblt, transactionCoder, sorter);
    }

    @Test
    public void testSimple() throws Exception {
        testEncodeDecode(0, 0);
    }

    @Test
    public void testOneExtraTx() throws Exception {
        testEncodeDecode(1, 0);
    }

    @Test
    public void testOneAbsentTx() throws Exception {
        testEncodeDecode(0, 1);
    }

    @Test
    public void testExtraAndAbsentTx() throws Exception {
        testEncodeDecode(1, 1);
    }

    @Test
    public void testTwoExtraAndAbsentTx() throws Exception {
        testEncodeDecode(2, 2);
    }

    @Test
    public void testFiveExtraAndAbsentTx() throws Exception {
        testEncodeDecode(5, 5);
    }

    @Test
    public void testFiftyExtraAndAbsentTx() throws Exception {
        testEncodeDecode(50, 50);
    }

    private void testEncodeDecode(int extraCount, int absentCount) {
        Block block = getBlock();
        IBLT<LongData, LongData> iblt = sut.encode(block);
        List<Transaction> blockTransactions = block.getTransactions();

        List<Transaction> myTransactions = getMyTransactions(blockTransactions, extraCount, absentCount);

        Block result = sut.decode(block.cloneAsHeader(), iblt, myTransactions);
        assertNotNull(result);
        assertEquals(block, result);
    }

}
