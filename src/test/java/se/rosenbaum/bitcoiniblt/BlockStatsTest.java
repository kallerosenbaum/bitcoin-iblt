package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.*;
import com.google.bitcoin.params.MainNetParams;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.bytearraydata.ByteArrayDataTransactionCoder;
import se.rosenbaum.bitcoiniblt.bytearraydata.IBLTUtils;
import se.rosenbaum.iblt.IBLT;

import java.io.*;
import java.util.ArrayList;
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
    private static Logger logger = LoggerFactory.getLogger(BlockStatsTest.class);

    private TransactionSorter sorter;
    private BlockCoder sut;

    private static final int DIVISOR = 60;
    public static final String OUTPUT_FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s\n";
    public static final String LOGGER_FORMAT = "{},{},{},{},{},{},{},{},{}\n";

    @Before
    public void setup() {
        sorter = new CanonicalOrderTransactionSorter();
    }

    private void createBlockCoder(int cellCount, int hashFunctionCount, int keySize, int valueSize) {
        TransactionCoder transactionCoder = new ByteArrayDataTransactionCoder(getParams(), salt, keySize, valueSize);
        IBLT iblt = new IBLTUtils().createIblt(cellCount, hashFunctionCount, keySize, valueSize, 4);
        sut = new BlockCoder(iblt, transactionCoder, sorter);
    }

    private static class BlockStatsResult {
        boolean success;
        long encodingTime;
        long decodingTime;
    }

    private static class TestConfig {
        int txCount;
        int hashFunctionCount;
        int keySize;
        int valueSize;
        int keyHashSize;
        int cellCount;
    }

    @Test
    public void testValueSizeVsCellCount() throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(new File(tempDirectory, "valueSize-cellCount-Stats.csv")));
        writer.printf(OUTPUT_FORMAT, "txcount", "hashFunctionCount", "keySize", "valueSize", "keyHashSize", "cellCount",
                "encodeTime", "decodeTime");
        int txCount = 3000;
        int hashFunctionCount = 4;

        int currentAttempt = 32384;
        int lowestSuccess = currentAttempt;
        int highestFail = 0;

        for (int i = 0; i < 10; i++) {
            int valueSize = 8*(int)Math.pow(2, i); // 8 --> 4096 bytes

            boolean finished = false;
            BlockStatsResult lastSuccess = null;
            while (!finished) {
                createBlockCoder(currentAttempt, hashFunctionCount, 8, valueSize);

                BlockStatsResult result = testBlockStats(txCount);
                logger.info(LOGGER_FORMAT, txCount, hashFunctionCount, 8, valueSize, 4, currentAttempt,
                        result.encodingTime, result.decodingTime, result.success ? "success" : "fail");
                if (result.success) {
                    lastSuccess = result;
                    lowestSuccess = currentAttempt;
                } else {
                    highestFail = currentAttempt;
                }
                currentAttempt = lowestSuccess - (lowestSuccess - highestFail)/2;
                // Must be a multiple of hashFunctionCount;
                currentAttempt += currentAttempt % hashFunctionCount;

                if (currentAttempt >= lowestSuccess || currentAttempt <= highestFail) {
                    finished = true;
                    currentAttempt = lowestSuccess;
                    writer.printf(OUTPUT_FORMAT, txCount, hashFunctionCount, 8, valueSize, 4, currentAttempt,
                            lastSuccess.encodingTime, lastSuccess.decodingTime);
                    writer.flush();
                    highestFail = 0;
                }
            }
        }
        writer.close();
    }

    public BlockStatsResult testBlockStats(int transactionCount) throws IOException {
        TransactionCollectorProcessor transactionCollector = new TransactionCollectorProcessor();
        int diffCount = transactionCount / DIVISOR;
        transactionCollector.count = transactionCount + diffCount;

        processTransactions(MAINNET_BLOCK, Integer.MAX_VALUE, transactionCollector);
        List<Transaction> blockTransactions = transactionCollector.transactions;
        List<Transaction> myTransactions = createDifferences(blockTransactions, diffCount);

        List<Transaction> sortedBlockTransactions = sorter.sort(blockTransactions);

        Block myBlock = EasyMock.createMock(Block.class);
        expect(myBlock.getTransactions()).andReturn(sortedBlockTransactions);

        Block recreatedBlock = EasyMock.createMock(Block.class);
        Capture<Transaction> capture = new Capture<Transaction>(CaptureType.ALL);
        recreatedBlock.addTransaction(EasyMock.capture(capture));
        expectLastCall().anyTimes();

        replay(recreatedBlock, myBlock);

        BlockStatsResult result = new BlockStatsResult();

        long startTime = System.currentTimeMillis();
        IBLT iblt = sut.encode(myBlock);
        result.encodingTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        Block resultBlock = sut.decode(recreatedBlock, iblt, myTransactions);
        result.decodingTime = System.currentTimeMillis() - startTime;

        if (resultBlock == null) {
            result.success = false;
            return result;
        }

        List<Transaction> decodedTransaction = capture.getValues();
        assertListsEqual(sortedBlockTransactions, decodedTransaction);
        result.success = true;
        return result;
    }

    private List<Transaction> createDifferences(List<Transaction> blockTransactions, int diffCount) {
        List<Transaction> result = new ArrayList<Transaction>(blockTransactions);
        if (diffCount == 0) {
            return result;
        }
        assertTrue(diffCount < blockTransactions.size());
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
            if (transaction.isCoinBase()) {
                if (transactions.size() == 0) {
                    transactions.add(transaction);
                } else {
                    return;
                }
            } else {
                transactions.add(transaction);
            }
            if (transactions.size() == count) {
                throw new StopProcessingException();
            }
        }
    }

    public NetworkParameters getParams() {
        return MainNetParams.get();
    }

}
