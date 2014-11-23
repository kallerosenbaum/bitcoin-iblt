package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Transaction;
import org.junit.Before;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.longdata.IBLTUtils;
import se.rosenbaum.bitcoiniblt.longdata.LongDataTransactionCoder;
import se.rosenbaum.bitcoiniblt.util.TransactionProcessor;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BlockClientCoderTest extends ClientCoderTest {

    private BlockCoder sut;

    @Before
    public void setup() {
        sut = createBlockCoder(new IBLTUtils().createIblt(1600, 4));
    }

    private BlockCoder createBlockCoder(IBLT iblt) {
        TransactionCoder transactionCoder = new LongDataTransactionCoder(getParams(), salt);
        SameAsBlockTransactionSorter sorter = new SameAsBlockTransactionSorter(getBlock());
        return new BlockCoder(iblt, transactionCoder, sorter);
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
    public void testMaxExtraAndAbsentTx() throws Exception {
        // This is the maximum amount of differences I can get
        testEncodeDecode(18, 20);
    }

    @Test
    public void testMoreThanMaxExtraTx() throws Exception {
        // This is the maximum amount of differences I can get
        testEncodeDecode(19, 20, false);
    }

    @Test
    public void testMoreThanMaxAbsentTx() throws Exception {
        // This is the maximum amount of differences I can get
        testEncodeDecode(18, 21, false);
    }

    private void testEncodeDecode(int extraCount, int absentCount) {
        testEncodeDecode(extraCount, absentCount, true);
    }

    private void testEncodeDecode(int extraCount, int absentCount, boolean expectSuccess) {
        Block block = getBlock();
        IBLT<LongData, LongData> iblt = sut.encode(block);
        List<Transaction> blockTransactions = block.getTransactions();

        List<Transaction> myTransactions = getMyTransactions(blockTransactions, extraCount, absentCount);

        sut = createBlockCoder(iblt);

        Block result = sut.decode(block.cloneAsHeader(), myTransactions);
        if (expectSuccess) {
            assertNotNull(result);
            assertEquals(block, result);
        } else {
            assertNull(result);
        }

    }

    protected List<Transaction> getMyTransactions(List<Transaction> blockTransactions, int extraCount, int absentCount) {
        List<Transaction> myTransactions = new ArrayList<Transaction>(blockTransactions.size());
        // Mess up the ordering
        for (int i = blockTransactions.size() - 1; i >= 0; i--) {
            myTransactions.add(blockTransactions.get(i));
        }

        // Remove some transactions from my "mempool"
        assertTrue(extraCount <= myTransactions.size());
        for (int i = 0; i < extraCount; i++) {
            myTransactions.remove(myTransactions.size() - 2 - (i*(myTransactions.size()-2)/extraCount));
        }

        // Add some transactions to my "mempool" that is not in the IBLT
        TransactionAdder txAdder = new TransactionAdder(myTransactions);
        processTransactions(getBlock().getPrevBlockHash().toString(), absentCount, txAdder);

        return myTransactions;
    }

    private class TransactionAdder implements TransactionProcessor {
        private List<Transaction> transactions;

        private TransactionAdder(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        @Override
        public void process(Transaction transaction) {
            transactions.add(transaction);
        }
    }

}
