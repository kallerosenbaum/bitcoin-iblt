package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.bytearraydata.ByteArrayDataTransactionCoder;
import se.rosenbaum.bitcoiniblt.bytearraydata.IBLTUtils;
import se.rosenbaum.bitcoiniblt.chart.BarChart;
import se.rosenbaum.iblt.IBLT;

import javax.imageio.ImageIO;
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

    public static final String OUTPUT_FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s,%s\n";
    public static final String LOGGER_FORMAT = "{},{},{},{},{},{},{},{},{}";
    private PrintWriter writer;

    @Before
    public void setup() {
        sorter = new CanonicalOrderTransactionSorter();
    }

    private void createBlockCoder(TestConfig config) {
        TransactionCoder transactionCoder = new ByteArrayDataTransactionCoder(getParams(), salt, config.keySize,
                config.valueSize);
        IBLT iblt = new IBLTUtils().createIblt(config.cellCount, config.hashFunctionCount, config.keySize,
                config.valueSize, config.keyHashSize);
        sut = new BlockCoder(iblt, transactionCoder, sorter);
    }

    private static class BlockStatsResult {
        boolean success;
        long encodingTime;
        long decodingTime;
    }

    private static class TestConfig {
        int txCount;
        int extraTxCount;
        int absentTxCount;
        int hashFunctionCount;
        int keySize;
        int valueSize;
        int keyHashSize;
        int cellCount;
        private TestConfig(int txCount, int extraTxCount, int absentTxCount, int hashFunctionCount, int keySize, int valueSize, int keyHashSize, int cellCount) {
            this.txCount = txCount;
            this.extraTxCount = extraTxCount;
            this.absentTxCount = absentTxCount;
            this.hashFunctionCount = hashFunctionCount;
            this.keySize = keySize;
            this.valueSize = valueSize;
            this.keyHashSize = keyHashSize;
            this.cellCount = cellCount;
        }

        public int getIbltSize() {
            return cellCount * (keySize + valueSize + keyHashSize + 4/*counter*/);
        }
    }

    private static class TransactionSets {
        List<Transaction> sendersTransactions;
        List<Transaction> receiversTransactions;
    }

    private void logResult(TestConfig config, BlockStatsResult result) {
        logger.info(LOGGER_FORMAT, config.txCount, config.hashFunctionCount, config.keySize, config.valueSize,
                config.keyHashSize, config.cellCount,
                result.encodingTime, result.decodingTime, result.success ? "success" : "fail");
    }
    private void printResult(TestConfig config, BlockStatsResult result) {
        writer.printf(OUTPUT_FORMAT, config.txCount, config.hashFunctionCount, config.keySize,
                config.valueSize, config.keyHashSize,
                config.cellCount, result.encodingTime, result.decodingTime, config.getIbltSize());
        writer.flush();
    }

    private static class Interval {
        int low;
        int high;

        private Interval(int low, int high) {
            this.low = low;
            this.high = high;
        }

        private int nextValue(TestConfig config) {
            int next = high - (high - low)/2;
            // Must be a multiple of hashFunctionCount;
            return next - next % config.hashFunctionCount;
        }
        private boolean isInsideInterval(int value) {
            return value < high && value > low;
        }
    }

    @Test
    public void testHashFunctionCountVsCellCount() throws IOException {
        String filePrefix = "hfCount-cellCount-Stats";
        setupResultPrinter(filePrefix + ".csv");

        int cellCountStart = 8192*2*2*2*2;
        TestConfig config = new TestConfig(50, 50, 50, 1, 8, 270, 4, cellCountStart);

        Interval interval = new Interval(0, config.cellCount);

        int minHashFunctionCount = 2;
        int maxHashFunctionCount = 10;
        int[] category = new int[maxHashFunctionCount-minHashFunctionCount + 1];
        int[] yValues = new int[category.length];

        for (int i = minHashFunctionCount; i <= maxHashFunctionCount; i++) {
            config.hashFunctionCount = i; // 8 --> 4096 bytes

            BlockStatsResult lastSuccessResult = null;
            interval.low = 0;
            interval.high = cellCountStart;
            // must be a multiple of hashFunctionCount
            config.cellCount = interval.high - interval.high%config.hashFunctionCount;

            while (true) {
                BlockStatsResult result = testBlockStats(config);
                logResult(config, result);
                if (result.success) {
                    lastSuccessResult = result;
                    interval.high = config.cellCount;
                } else {
                    interval.low = config.cellCount;
                }
                config.cellCount = interval.nextValue(config);

                if (!interval.isInsideInterval(config.cellCount)) {
                    config.cellCount = interval.high;
                    printResult(config, lastSuccessResult);
                    category[i-minHashFunctionCount] = i;
                    yValues[i-minHashFunctionCount] = config.getIbltSize();
                    break;
                }
            }
        }
        writer.close();
        createImage(filePrefix, category, yValues, "hash function count", "Minimum IBLT size [bytes]");
    }

    private void createImage(String fileName, int[] category, int[] yValues, String categoryCaption,
                             String valueCaption) throws IOException {
        BarChart barChart = new BarChart(category, yValues, categoryCaption, valueCaption);

        OutputStream out = new FileOutputStream(new File(tempDirectory, fileName + ".png"));
        try {
            ImageIO.write(barChart.getImage(), "png", out);
        } catch (IOException e) {
            logger.error("Failed to write image.", e);
        }
        out.close();
    }

    @Test
    public void testValueSizeVsCellCount() throws IOException {
        String filePrefix = "valueSize-cellCount-Stats";
        setupResultPrinter(filePrefix + ".csv");

        TestConfig config = new TestConfig(50, 50, 50, 4, 8, 8, 4, 32384);
        Interval interval = new Interval(0, config.cellCount);

        int[] category = new int[] {8, 16, 32, 64, 128, 256, 270, 280, 512, 1024, 2048};
        int[] yValues = new int[category.length];

        for (int i = 0; i < category.length; i++) {
            config.valueSize = category[i];

            BlockStatsResult lastSuccessResult = null;
            while (true) {
                BlockStatsResult result = testBlockStats(config);
                logResult(config, result);
                if (result.success) {
                    lastSuccessResult = result;
                    interval.high = config.cellCount;
                } else {
                    interval.low = config.cellCount;
                }
                config.cellCount = interval.nextValue(config);

                if (!interval.isInsideInterval(config.cellCount)) {
                    config.cellCount = interval.high;
                    printResult(config, lastSuccessResult);
                    yValues[i] = config.getIbltSize();
                    interval.low = 0;
                    break;
                }
            }
        }
        writer.close();
        createImage(filePrefix, category, yValues, "value size [B]", "Minimum IBLT size [B]");
    }

    private void setupResultPrinter(String fileName) throws IOException {
        writer = new PrintWriter(new FileWriter(new File(tempDirectory, fileName)));
        writer.printf(OUTPUT_FORMAT, "txcount", "hashFunctionCount", "keySize [B]", "valueSize [B]", "keyHashSize [B]",
                "cellCount",
                "encodeTime [ms]", "decodeTime [ms]", "minIBLTSize [B]");
    }

    public BlockStatsResult testBlockStats(TestConfig config) throws IOException {
        createBlockCoder(config);
        TransactionCollectorProcessor transactionCollector = new TransactionCollectorProcessor();
        transactionCollector.count = config.txCount + config.absentTxCount;

        processTransactions(MAINNET_BLOCK, Integer.MAX_VALUE, transactionCollector);
        List<Transaction> blockTransactions = transactionCollector.transactions;
        TransactionSets sets = createDifferences(blockTransactions, config);

        List<Transaction> sortedBlockTransactions = sorter.sort(sets.sendersTransactions);

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
        Block resultBlock = sut.decode(recreatedBlock, iblt, sets.receiversTransactions);
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

    private TransactionSets createDifferences(List<Transaction> collectedTransactions, TestConfig config) {
        assertTrue(config.txCount + config.absentTxCount == collectedTransactions.size());
        assertTrue(config.extraTxCount + config.absentTxCount <= collectedTransactions.size());

        TransactionSets sets = new TransactionSets();
        sets.receiversTransactions = new ArrayList<Transaction>(collectedTransactions);
        sets.sendersTransactions = new ArrayList<Transaction>(collectedTransactions);
        List<Transaction> send = sets.sendersTransactions;
        List<Transaction> rec = sets.receiversTransactions;

        send.removeAll(send.subList(rec.size() - config.absentTxCount, rec.size()));
        rec.removeAll(rec.subList(0, config.extraTxCount));
//        send.removeAll(send.subList(1, config.absentTxCount + 1));
//        rec.removeAll(rec.subList(rec.size() - config.extraTxCount, rec.size()));
//        for (int i = 0; i < config.extraTxCount; i++) {
//            int removalIndex = i * 2;
//            rec.remove(removalIndex); // When i == 0, this will remove the coinbase tx, which is realistic since
//            // the receiver cannot possibly have it.
//            send.remove(removalIndex + 1);
//
//        }

        return sets;
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
