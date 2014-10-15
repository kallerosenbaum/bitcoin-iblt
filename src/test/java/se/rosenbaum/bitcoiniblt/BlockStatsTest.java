package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.*;
import com.google.bitcoin.params.MainNetParams;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.bytearraydata.ByteArrayDataTransactionCoder;
import se.rosenbaum.bitcoiniblt.bytearraydata.IBLTUtils;
import se.rosenbaum.iblt.IBLT;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

/**
 * What do I want to accomplish?
 * <p/>
 * 1. Find good values of key, value and hashSum sizes
 * 2. Find a good number of hash functions (k)
 * 3. Find heuristics for selecting number of cells (m)
 * <p/>
 * Loose thoughts:
 * <p/>
 * If we double the the value size, we could roughly half the number of cells, If so, the
 * total size of the iblt will decrease by (oldCellCount / 2) * (hashSumSize + keySize).
 * <p/>
 * If the value size is increased to something larger than or equal to 100000/256 bytes ~= 391 bytes
 * then we can use a single byte as indexCounter, since the maximum tx size is 100000 B. But
 * I haven't seen this limit anywhere else than in Gavin's paper on O(1) block propagation and in
 * bitcoinj:s Transaction.MAX_STANDARD_TX_SIZE.
 * <p/>
 * We probably want a value size less than say, 270, since most transactions seems to be between 200 and 270 bytes.
 * Then they seem to jump up to about 380 bytes and upwards from there. At least on my sample of the latest
 * 10000 transactions in the block chain from block 0000000000000000152c3db4fe011716c4e5d41e44089e1da9d7d64917bb5011
 * and backwards
 * <p/>
 * How do we make experiments on this? I guess there are already good research done on sizing k and m. So I suppose
 * we should focus on the key, value and hashSum sizes. I guess our goal is to make the iblt as small as possible
 * while still keeping the probability of failure low (<0.001).     '
 * <p/>
 * Let's figure out a guesstimate on how big the differences between my transaction set and a possible receiver's
 * transaction set could be. I'm going to count high, to be on the safe side.
 * <p/>
 * Given a transaction propagation time of 4s (http://bitcoinstats.com/network/propagation/) for the transaction to
 * reach 90% of the nodes, we can assume that within 10 s the transaction have reached almost all nodes,
 * so it seems reasonable to assume that we have at most about 10s worth of transactions that the receiver of the
 * IBLT doesn't have, and vice versa, about 10s worth of transactions that the receiver have that I don't have. There
 * may also be older transactions that differ, but the vast majority of the differences are within the last 10 seconds.
 * How much is 10 seconds worth of transactions? With 600 seconds between blocks, that should be about the number of
 * transactions in the block / 60,
 * <p/>
 * So for a block with 1200 transactions, the difference should be about 20 plus-transactions and 20
 * minus-transactions. Unfortunately, we can't find blocks containing arbitrary amounts of transactions, so
 * we'll have to fake them, but we can still use real world transactions.
 */
public class BlockStatsTest extends ClientCoderTest {

    private TransactionCoder transactionCoder;
    private TransactionSorter sorter;
    private BlockCoder sut;

    @Before
    public void setup() {
        transactionCoder = new ByteArrayDataTransactionCoder(getParams(), salt, 8, 32);
        sorter = new CanonicalOrderTransactionSorter();
        IBLT iblt = new IBLTUtils().createIblt(1600, 4, 8, 32, 4);
        sut = new BlockCoder(iblt, transactionCoder, sorter);
    }

    @Test
    public void testBlockStats() throws IOException {
        TransactionCollectorProcessor transactionCollector = new TransactionCollectorProcessor();
        int blockTransactionCount = 100;
        int diffCount = blockTransactionCount / 60;
        transactionCollector.count = blockTransactionCount + diffCount;

        processTransactions(MAINNET_BLOCK, Integer.MAX_VALUE, transactionCollector);
        List<Transaction> blockTransactions = transactionCollector.transactions;
        List<Transaction> myTransactions = fixDifferences(blockTransactions, diffCount);

        List<Transaction> sortedBlockTransactions = new ArrayList<Transaction>(blockTransactions);
        sorter.sort(sortedBlockTransactions);

        Block myBlock = EasyMock.createMock(Block.class);
        expect(myBlock.getTransactions()).andReturn(sortedBlockTransactions);

        Block recreatedBlock = EasyMock.createMock(Block.class);
        Capture<Transaction> capture = new Capture<Transaction>(CaptureType.ALL);
        recreatedBlock.addTransaction(EasyMock.capture(capture));
        expectLastCall().anyTimes();

        replay(recreatedBlock, myBlock);

        IBLT iblt = sut.encode(myBlock);
        Block resultBlock = sut.decode(recreatedBlock, iblt, myTransactions);
        assertNotNull(resultBlock);

        List<Transaction> result = capture.getValues();
        assertListsEqual(sortedBlockTransactions, result);
    }

    private List<Transaction> fixDifferences(List<Transaction> blockTransactions, int diffCount) {
        List<Transaction> result = new ArrayList<Transaction>(blockTransactions);
        assertTrue(diffCount < blockTransactions.size() - 2);
        // Remove different transactions from both result and blockTransactions
        for (int i = 0; i < diffCount; i++) {
            int removalIndex = i * 2;
            result.remove(removalIndex); // When i == 0, this will remove the coinbase tx, which is realistic since
                                         // the receiver cannot possibly have it.
            blockTransactions.remove(removalIndex + 1);
        }
        return result;
    }

    private void assertListsEqual(List expected, List actual) {
        if (expected == null && actual == null) {
            return;
        }
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        assertArrayEquals(expected.toArray(new Object[0]), actual.toArray(new Object[0]));
    }

    private class TransactionCollectorProcessor implements TransactionProcessor {
        private List<Transaction> transactions = new ArrayList<Transaction>();
        private int count;
        @Override
        public void process(Transaction transaction) throws StopProcessingException {
            if (transactions.size() == 0 && transaction.isCoinBase()) {
                transactions.add(transaction);
            }
            if (transactions.size() > 0 && transaction.isCoinBase()) {
                return;
            }
            transactions.add(transaction);
            if (transactions.size() == count) {
                throw new StopProcessingException();
            }
        }
    }

    public NetworkParameters getParams() {
        return MainNetParams.get();
    }

}
