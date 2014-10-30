package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.bytearraydata.ByteArrayDataTransactionCoder;
import se.rosenbaum.bitcoiniblt.bytearraydata.IBLTUtils;
import se.rosenbaum.bitcoiniblt.printer.BlockStatsPrinter;
import se.rosenbaum.bitcoiniblt.printer.HashCountCellCountPrinter;
import se.rosenbaum.bitcoiniblt.printer.ValueSizeCellCountPrinter;
import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.Interval;
import se.rosenbaum.bitcoiniblt.util.ResultStats;
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.util.TransactionCollectorProcessor;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;
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
    private TransactionSorter sorter;
    private BlockCoder sut;

    @Before
    public void setup() {
        sorter = new CanonicalOrderTransactionSorter();
    }

    private void createBlockCoder(TestConfig config) {
        TransactionCoder transactionCoder = new ByteArrayDataTransactionCoder(getParams(), salt, config.getKeySize(),
                config.getValueSize());
        IBLT iblt = new IBLTUtils().createIblt(config.getCellCount(), config.getHashFunctionCount(), config.getKeySize(),
                config.getValueSize(), config.getKeyHashSize());
        sut = new BlockCoder(iblt, transactionCoder, sorter);
    }

    @Test
    public void testHashFunctionCountVsCellCount() throws IOException {
        int cellCountStart = 8192*2*2*2*2;
        TestConfig config = new TestConfig(50, 50, 50, 1, 8, 270, 4, cellCountStart, false);

        Interval interval = new Interval(0, config.getCellCount());

        int minHashFunctionCount = 2;
        int maxHashFunctionCount = 10;
        BlockStatsPrinter printer = new HashCountCellCountPrinter(tempDirectory, "hfCount-cellCount-Stats", maxHashFunctionCount-minHashFunctionCount + 1);

        for (int i = minHashFunctionCount; i <= maxHashFunctionCount; i++) {
            config.setHashFunctionCount(i);

            BlockStatsResult lastSuccessResult = null;
            interval.setLow(0);
            interval.setHigh(cellCountStart);
            // must be a multiple of hashFunctionCount
            config.setCellCount(interval.getHigh() - interval.getHigh() % config.getHashFunctionCount());

            while (true) {
                BlockStatsResult result = testBlockStats(config);
                printer.logResult(config, result);
                if (result.isSuccess()) {
                    lastSuccessResult = result;
                    interval.setHigh(config.getCellCount());
                } else {
                    interval.setLow(config.getCellCount());
                }
                config.setCellCount(interval.nextValue(config));

                if (!interval.isInsideInterval(config.getCellCount())) {
                    config.setCellCount(interval.getHigh());
                    printer.addResult(config, lastSuccessResult);
                    break;
                }
            }
        }
        printer.close();
    }

    @Test
    public void testValueSizeVsCellCount() throws IOException {
        TestConfig config = new TestConfig(50, 50, 50, 4, 8, 8, 4, 32384, false);
        Interval interval = new Interval(0, config.getCellCount());

        int[] category = new int[] {8, 16, 32, 64, 128, 256, 270, 280, 512, 1024, 2048};
        BlockStatsPrinter writer = new ValueSizeCellCountPrinter(tempDirectory, "valueSize-cellCount-Stats", category.length);

        for (int i = 0; i < category.length; i++) {
            config.setValueSize(category[i]);

            BlockStatsResult lastSuccessResult = null;
            while (true) {
                BlockStatsResult result = testBlockStats(config);
                writer.logResult(config, result);
                if (result.isSuccess()) {
                    lastSuccessResult = result;
                    interval.setHigh(config.getCellCount());
                } else {
                    interval.setLow(config.getCellCount());
                }
                config.setCellCount(interval.nextValue(config));

                if (!interval.isInsideInterval(config.getCellCount())) {
                    config.setCellCount(interval.getHigh());
                    writer.addResult(config, lastSuccessResult);
                    interval.setLow(0);
                    break;
                }
            }
        }
        writer.close();
    }

    @Test
    public void testCellCountVSFailureProbability() throws IOException {
        String filePrefix = "cellCount-failureProbability-Stats";

        PrintWriter writer = new PrintWriter(new FileWriter(new File(tempDirectory, filePrefix + ".csv")));
        String format = "%s,%s,%s,%s,%s,%s,%s,%s,%s\n";
        writer.printf(format, "txcount", "hashFunctionCount", "keySize [B]", "valueSize [B]", "keyHashSize [B]",
                "cellCount", "failureCount", "successCount", "failureProbability");

        int startCellCount = 2700;
        TestConfig config = new TestConfig(500, 500, 500, 3, 8, 270, 4, 0, true);

        int dataPoints = 50;

        List<ResultStats> resultStatsList = new ArrayList<ResultStats>(dataPoints);

        for (int i = 0; i < dataPoints; i++) {
            config.setCellCount(startCellCount + i*60);
            ResultStats resultStats = testCellCountVSFailureProbability(config);
            resultStatsList.add(resultStats);
            writer.printf(format, config.getTxCount(), config.getHashFunctionCount(), config.getKeySize(),
                    config.getValueSize(), config.getKeyHashSize(), config.getCellCount(), resultStats.getFailures(), resultStats.getSuccesses(), resultStats.getFailures() / (resultStats.getFailures() + resultStats.getSuccesses()));
            writer.flush();
        }
        writer.close();
    }

    private ResultStats testCellCountVSFailureProbability(TestConfig config) throws IOException {
        ResultStats stats = new ResultStats(config);

        for (int i = 0; i < 10000; i++) {
            BlockStatsResult result = testBlockStats(config);
            if (result.isSuccess()) {
                stats.setSuccesses(stats.getSuccesses() + 1);
            } else {
                stats.setFailures(stats.getFailures() + 1);
            }
            //if (i % 1000 == 0) logResult(config, result);
        }
        return stats;
    }

    public BlockStatsResult testBlockStats(TestConfig config) throws IOException {
        createBlockCoder(config);
        TransactionSets sets = createTransactionSets(config);

        List<Transaction> sortedBlockTransactions = sorter.sort(sets.getSendersTransactions());

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
        result.setEncodingTime(System.currentTimeMillis() - startTime);

        startTime = System.currentTimeMillis();
        Block resultBlock = sut.decode(recreatedBlock, iblt, sets.getReceiversTransactions());
        result.setDecodingTime(System.currentTimeMillis() - startTime);

        if (resultBlock == null) {
            result.setSuccess(false);
            return result;
        }

        List<Transaction> decodedTransaction = capture.getValues();
        assertListsEqual(sortedBlockTransactions, decodedTransaction);
        result.setSuccess(true);
        return result;
    }

    private TransactionSets createTransactionSets(TestConfig config) {
        List<Transaction> collectedTransactions;
        if (config.isRandomTxSelection()) {
            collectedTransactions = getRandomTransactions(config.getTxCount() + config.getAbsentTxCount());
        } else {
            collectedTransactions = getSameOldTransactions(config.getTxCount() + config.getAbsentTxCount());
        }

        assertTrue(config.getTxCount() + config.getAbsentTxCount() == collectedTransactions.size());
        assertTrue(config.getExtraTxCount() + config.getAbsentTxCount() <= collectedTransactions.size());

        TransactionSets sets = new TransactionSets();
        sets.setReceiversTransactions(new ArrayList<Transaction>(collectedTransactions));
        sets.setSendersTransactions(new ArrayList<Transaction>(collectedTransactions));
        List<Transaction> send = sets.getSendersTransactions();
        List<Transaction> rec = sets.getReceiversTransactions();

        send.removeAll(send.subList(rec.size() - config.getAbsentTxCount(), rec.size()));
        rec.removeAll(rec.subList(0, config.getExtraTxCount()));

        assertEquals(config.getTxCount(), send.size());
        assertEquals(config.getTxCount(), rec.size());
        for (Transaction transaction : rec) {
            assertFalse(send.contains(transaction));
        }

        return sets;
    }

    private List<Transaction> getSameOldTransactions(int txCount) {
        TransactionCollectorProcessor transactionCollector = new TransactionCollectorProcessor(txCount);

        processTransactions(MAINNET_BLOCK, Integer.MAX_VALUE, transactionCollector);
        return transactionCollector.getTransactions();
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

    public NetworkParameters getParams() {
        return MainNetParams.get();
    }

}
