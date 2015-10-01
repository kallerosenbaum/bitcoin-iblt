package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Transaction;
import org.junit.Before;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.BlockStatsClientCoderTest;
import se.rosenbaum.bitcoiniblt.printer.FailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.printer.IBLTSizeVsFailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.Interval;
import se.rosenbaum.bitcoiniblt.util.ResultStats;
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CorpusDataTestManual extends BlockStatsClientCoderTest {

    private CorpusData corpusData;

    @Before
    public void setup() {
        super.setup();
        corpusData = new CorpusData(new File("/home/kalle/hack/git/rusty/bitcoin-corpus"));
        MAINNET_BLOCK = CorpusData.HIGHEST_BLOCK_HASH;

    }

    @Test
    public void testAU() throws IOException {
        RecordInputStream recordInputStream = corpusData.getRecordInputStream(CorpusData.Node.AU);
        Record record = recordInputStream.readRecord();
        while (record != null) {
            System.out.println(record.toString());
            record = recordInputStream.readRecord();
        }
    }

    @Test
    public void testAUListBlocks() throws IOException {
        RecordInputStream recordInputStream = corpusData.getRecordInputStream(CorpusData.Node.AU);
        Record record = recordInputStream.readRecord();
        while (record != null) {
            if (Type.COINBASE == record.type) {
                System.out.println(record.toString());
            }
            record = recordInputStream.readRecord();
        }
    }

    /**
     Each record either reflects a new TX we accepted into the mempool from
     the network, OR the relative state of the mempool when a new block
     came in:

     [4-byte:timestamp] [1 byte:type] [3-byte:block number] [32-byte:txid]

     Where type is:
     1. INCOMING_TX: a new transaction added to the mempool, block number is 0.
     2. COINBASE: a coinbase transaction (ie. we didn't know this one)
     3. UNKNOWN: a transaction was in the new block, and we didn't know about it.
     4. KNOWN: a transaction was in the new block, and our mempool.
     5. MEMPOOL_ONLY: a transaction was in the mempool, but not the block.

     The ordering is:
     1. Zero or more INCOMING_TX.
     2. A COINBASE tx.
     3. Zero or more UNKNOWN and KNOWN txs, in any order.
     4. Zero or more MEMPOOL_ONLY txs.

     You can simply uncompress the corpora and load them directly into C
     arrays.  See example/simple-analysis.c.

     */

    /*
    1. Collect all extras (not coinbases) for all blocks and nodes from corpus. Calculate the average extras, E, over the remaining.
    2. Calculate the average tx rate, R, over the corpus. Sum the number of transactions in all blocks and divide it with the data collection period in seconds.
    3. Now calculate the "extras per tx rate", E/R.
    4. Absents, A, is calculated from E and the ratio extras/absent
    5. Assume that E/R is constant and that the extras/absent ratio holds for all tx rates.
    */
    @Test
    public void testExtrasPerTxRate() throws IOException {
        corpusData.calculateStatistics();

        System.out.println("Average transaction rate  : " + corpusData.txRate);
        System.out.println("Average extras per block  : " + corpusData.averageExtrasPerBlock);
        System.out.println("Average extras per tx rate: " + corpusData.extrasPerTxRate);
    }


    @Test
    public void testFactor1() throws IOException {
        corpusData.calculateStatistics();
        blockCount = corpusData.blockCount;

        int factor = 1000;
        int sampleCount = 1000;

        int extras = (int) Math.ceil(corpusData.averageExtrasPerBlock) * factor;

        FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);

        CorpusDataTestConfig testConfig = new CorpusDataTestConfig(extras, extras, 100002);

        Interval interval = new Interval(0, testConfig.getCellCount());
        while (true) {
            ResultStats result = testFailureProbability(printer, testConfig, sampleCount);

            if (result.getFailureProbability() > 0.02 && result.getFailureProbability() < 0.1) {
                printer.addResult(testConfig, result);
            }

            if (result.getFailureProbability() < 0.05) {
                interval.setHigh(testConfig.getCellCount());
            } else {
                interval.setLow(testConfig.getCellCount());
            }
            testConfig = new CorpusDataTestConfig(extras, extras, interval.nextValue(testConfig));

            if (!interval.isInsideInterval(testConfig.getCellCount())) {
                break;
            }
        }

        printer.finish();
    }

    @Test
    public void testGenerateTestFileFactor1() throws IOException {
        corpusData.calculateStatistics();
        blockCount = corpusData.blockCount;

        int factor = 1000;
        int sampleCount = 1000;

        int extras = (int) Math.ceil(corpusData.averageExtrasPerBlock) * factor;

        CorpusDataTestConfig testConfig = new CorpusDataTestConfig(extras, extras, 100002);


    }


    private class CorpusDataTestConfig extends TestConfig {

        public CorpusDataTestConfig(int extraTxCount, int absentTxCount, int cellCount) {
            super(0, extraTxCount, absentTxCount, 3, 8, 64, 4, cellCount);
        }

        @Override
        public TransactionSets createTransactionSets() {
            List<Transaction> randomTransactions = getRandomTransactions(getExtraTxCount() + getAbsentTxCount());
            TransactionSets transactionSets = new TransactionSets();
            // As with most other tests, we just care about differences. Transactions that are both in sender's and
            // receiver's transacitons will just be added and deleted so they don't affect the result.
            transactionSets.setSendersTransactions(randomTransactions.subList(0, getExtraTxCount()));
            transactionSets.setReceiversTransactions(randomTransactions.subList(getExtraTxCount(), getExtraTxCount() + getAbsentTxCount()));
            return transactionSets;
        }
    }
}