package se.rosenbaum.bitcoiniblt;

import org.junit.Test;
import se.rosenbaum.bitcoiniblt.printer.HashCountCellCountPrinter;
import se.rosenbaum.bitcoiniblt.printer.IBLTSizeBlockStatsPrinter;
import se.rosenbaum.bitcoiniblt.printer.ValueSizeCellCountPrinter;
import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.Interval;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.IOException;

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
public class BlockStatsRun extends BlockStatsClientCoderTest {

    @Test
    public void testHashFunctionCountVsCellCount() throws IOException {
        int cellCountStart = 8192 * 2 * 2 * 2 * 2;
        TestConfig config = new TestConfig(50, 50, 50, 1, 8, 270, 4, cellCountStart, false);

        Interval interval = new Interval(0, config.getCellCount());

        int minHashFunctionCount = 2;
        int maxHashFunctionCount = 10;
        IBLTSizeBlockStatsPrinter printer = new HashCountCellCountPrinter(tempDirectory, maxHashFunctionCount - minHashFunctionCount + 1);

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
        printer.finish();
    }

    @Test
    public void testValueSizeVsCellCount() throws IOException {
        TestConfig config = new TestConfig(50, 50, 50, 4, 8, 8, 4, 32384, false);
        Interval interval = new Interval(0, config.getCellCount());

        int[] category = new int[]{8, 16, 32, 64, 128, 256, 270, 280, 512, 1024, 2048};
        IBLTSizeBlockStatsPrinter printer = new ValueSizeCellCountPrinter(tempDirectory, category.length);

        for (int i = 0; i < category.length; i++) {
            config.setValueSize(category[i]);

            BlockStatsResult lastSuccessResult = null;
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
                    interval.setLow(0);
                    break;
                }
            }
        }
        printer.finish();
    }



}
