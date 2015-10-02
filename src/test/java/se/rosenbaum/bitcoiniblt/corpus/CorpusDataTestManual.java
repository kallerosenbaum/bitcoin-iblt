package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

public class CorpusDataTestManual extends BlockStatsClientCoderTest {

    private CorpusData corpusStats;
    private File testFileDir;

    @Before
    public void setup() {
        super.setup();
        Properties props = new Properties();
        InputStream is = ClassLoader.getSystemResourceAsStream("junittests.properties");
        try {
            props.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String corpusHomePath = props.getProperty("corpus.directory");

        this.corpusStats = new CorpusData(new File(corpusHomePath));
        MAINNET_BLOCK = CorpusData.HIGHEST_BLOCK_HASH;

        testFileDir = new File(tempDirectory, "corpustestfiles");
        testFileDir.mkdirs();
    }

    @Test
    public void testAU() throws IOException {
        RecordInputStream recordInputStream = corpusStats.getRecordInputStream(CorpusData.Node.AU);
        Record record = recordInputStream.readRecord();
        while (record != null) {
            System.out.println(record.toString());
            record = recordInputStream.readRecord();
        }
    }

    @Test
    public void testAUListBlocks() throws IOException {
        RecordInputStream recordInputStream = corpusStats.getRecordInputStream(CorpusData.Node.AU);
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
        corpusStats.calculateStatistics();

        System.out.println("Average transaction rate  : " + corpusStats.txRate);
        System.out.println("Average extras per block  : " + corpusStats.averageExtrasPerBlock);
        System.out.println("Average extras per tx rate: " + corpusStats.extrasPerTxRate);
    }


    @Test
    public void testFactor1() throws IOException {
        corpusStats.calculateStatistics();
        blockCount = corpusStats.blockCount;

        int factor = 1;
        int sampleCount = 1000;

        int extras = (int) Math.ceil(corpusStats.averageExtrasPerBlock) * factor;

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
    public void testFromTestFile() throws IOException {
        int cellCount = 300;

        TestFileTestConfigGenerator configGenerator = null;
        for (int factor : new int[] {1, 10, 100, 1000}) {
            FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);
            if (factor > 1) {
                cellCount = configGenerator.getCellCount() * 10;
            }
            configGenerator = new TestFileTestConfigGenerator(getFile(factor), 3, 8, 64, 4, cellCount);

            Interval interval = new Interval(0, configGenerator.getCellCount());
            while (true) {
                ResultStats result = testFailureProbability(printer, configGenerator);
                printer.addResult(configGenerator, result);
                if (result.getFailureProbability() < 0.05) {
                    interval.setHigh(configGenerator.getCellCount());
                } else {
                    interval.setLow(configGenerator.getCellCount());
                }
                configGenerator = new TestFileTestConfigGenerator(getFile(factor), 3, 8, 64, 4, interval.nextValue(configGenerator));

                if (!interval.isInsideInterval(configGenerator.getCellCount())) {
                    break;
                }
            }
            printer.finish();
        }


    }

    protected ResultStats testFailureProbability(FailureProbabilityPrinter printer, TestFileTestConfigGenerator configGenerator) throws IOException {
        ResultStats stats = new ResultStats();

        TestConfig config = configGenerator.createNextTestConfig();
        int i = 1;
        while (config != null) {
            BlockStatsResult result = testBlockStats(config);
            stats.addSample(result);
            if (i % 100 == 0) {
                printer.logResult(config, stats);
            }
            config = configGenerator.createNextTestConfig();
            i++;
        }
        return stats;
    }

    private static class TestFilePrinter {
        Writer writer;
        private TestFilePrinter(Writer writer) {
            this.writer = writer;
        }

        private void printTransactionSets(TransactionSets sets) throws IOException {
            writer.write("absent:");
            writeTransactions(sets.getReceiversTransactions());
            writer.write("extra:");
            writeTransactions(sets.getSendersTransactions());
        }

        private void writeTransactions(List<Transaction> transactions) throws IOException {
            boolean first = true;
            for (Transaction transaction : transactions) {
                if (!first) {
                    writer.write(",");
                }
                first = false;
                writer.write(transaction.getHash().toString());
            }
            writer.write("\n");
        }
    }

    @Test
    public void testGenerateTestFileFactor1_10_100_1000() throws IOException {
        corpusStats.calculateStatistics();
        blockCount = corpusStats.blockCount;
        int sampleCount = 1000;

        for (int i = 0; i < 4; i++) {
            createTestFile(sampleCount, i);
        }
    }

    private static class IntPair {
        int unknowns = 0;
        int knowns = 0;
    }

    private static class AverageExtrasPercentile implements CorpusData.RecordHandler {
        Map<Integer, IntPair> blocks = new HashMap<Integer, IntPair>();
        IntPair currentBlock = new IntPair();
        int currentHeight = 0;

        public void handle(Record record) {
            if (record.type == Type.COINBASE) {
                blocks.put(currentHeight, currentBlock);
                currentBlock = new IntPair();
                currentHeight = record.blockNumber;
            } else if (record.type == Type.UNKNOWN) {
                currentBlock.unknowns++;
            } else if (record.type == Type.KNOWN) {
                currentBlock.knowns++;
            }
        }


    }

    @Test
    public void testFindPercentilesOfExtras() throws IOException {
        corpusStats.calculateStatistics();
        int averageExtras = (int) Math.ceil(corpusStats.averageExtrasPerBlock);
        int countBelowEqualAverageExtras = 0;
        int countMoreThanAverageExtras = 0;
        List<Integer> unknowns = new ArrayList<Integer>();
        for (CorpusData.Node node : CorpusData.Node.values()) {
            AverageExtrasPercentile handler = new AverageExtrasPercentile();
            corpusStats.getStats(node, handler);
            blockCount += handler.blocks.size();

            for (IntPair intPair : handler.blocks.values()) {
                unknowns.add(intPair.unknowns);
                if (intPair.unknowns > 0 ) {
                    System.out.println("Unknowns: " + intPair.unknowns);
                }
                if (intPair.unknowns <= averageExtras) {
                    countBelowEqualAverageExtras++;
                } else {
                    countMoreThanAverageExtras++;
                }
            }
        }

        System.out.println("Exact avg extras:" + corpusStats.averageExtrasPerBlock);
        System.out.println("Ceil of extras  :" + averageExtras);
        System.out.println("Count <= average: " + countBelowEqualAverageExtras);
        System.out.println("Count >  average: " + countMoreThanAverageExtras);
        System.out.println("Percentage below: " + 100 * countBelowEqualAverageExtras / (countBelowEqualAverageExtras + countMoreThanAverageExtras));

        Collections.sort(unknowns);
        System.out.println("Rough percentiles:");
        int size = unknowns.size();
        for (int i = 1; i <= 9; i++) {
            int percent = 10*i;
            System.out.println(percent + "% has <= " + unknowns.get(size * i/10 - 1) + " extras");
        }
        for (int i = 1; i <= 10; i++) {
            int percent = 90 + i;
            System.out.println(percent + "% has <= " + unknowns.get(size * (90 + i) / 100 - 1) + " extras");
        }
    }

    private void createTestFile(long sampleCount, long factorExponent) throws IOException {
        int factor = (int)Math.pow(10, factorExponent);
        int extras = (int) Math.ceil(corpusStats.averageExtrasPerBlock) * factor;
        CorpusDataTestConfig testConfig = new CorpusDataTestConfig(extras, extras, 100002);
        File file = getFile(factor);

        FileWriter fileWriter = new FileWriter(file);
        TestFilePrinter testFilePrinter = new TestFilePrinter(fileWriter);

        for (int i = 0; i < sampleCount; i++) {
            testFilePrinter.printTransactionSets(testConfig.createTransactionSets());
        }

        fileWriter.close();
    }

    private File getFile(long factor) {
        return new File(testFileDir, "test-factor-" + factor + ".txt");
    }

    private class TestFileTestConfigGenerator extends TestConfig {
        private final BufferedReader fileReader;


        public TestFileTestConfigGenerator(File file, int hashFunctionCount, int keySize, int valueSize, int keyHashSize, int cellCount) throws FileNotFoundException {
            super(0, 0, 0, hashFunctionCount, keySize, valueSize, keyHashSize, cellCount);
            this.fileReader = new BufferedReader(new FileReader(file));
        }

        public TestConfig createNextTestConfig() {
            TransactionSets nextTransactionSets = createNextTransactionSets();
            if (nextTransactionSets == null) {
                return null;
            }
            setExtraTxCount(nextTransactionSets.getSendersTransactions().size());
            setAbsentTxCount(nextTransactionSets.getReceiversTransactions().size());
            return new TransactionSetsTestConfig(nextTransactionSets, getHashFunctionCount(), getKeySize(), getValueSize(), getKeyHashSize(), getCellCount());
        }

        private TransactionSets createNextTransactionSets() {
            try {
                String line = fileReader.readLine();
                if (line == null) {
                    fileReader.close();
                    return null;
                }
                TransactionSets transactionSets = new TransactionSets();
                transactionSets.setSendersTransactions(new ArrayList<Transaction>());
                transactionSets.setReceiversTransactions(new ArrayList<Transaction>());
                line = line.substring("absent:".length());
                addTransactionsToList(line, transactionSets.getReceiversTransactions());

                line = fileReader.readLine();
                line = line.substring("extra:".length());
                addTransactionsToList(line, transactionSets.getSendersTransactions());
                return transactionSets;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void addTransactionsToList(String line, List<Transaction> transactions) throws IOException {
            StringTokenizer tokenizer = new StringTokenizer(line, ",");
            while (tokenizer.hasMoreTokens()) {
                String hashString = tokenizer.nextToken();
                Transaction transaction = getTransaction(new Sha256Hash(hashString));
                if (transaction == null) {
                    throw new RuntimeException("Couldn't find transaction " + hashString);
                }
                transactions.add(transaction);
            }
        }

        @Override
        public TransactionSets createTransactionSets() {
            return null;
        }
    }

    private class TransactionSetsTestConfig extends TestConfig {
        TransactionSets transactionSets;

        public TransactionSetsTestConfig(TransactionSets sets, int hashFunctionCount, int keySize, int valueSize, int keyHashSize, int cellCount) {
            super(0, 0, 0, hashFunctionCount, keySize, valueSize, keyHashSize, cellCount);
            this.transactionSets = sets;
            setAbsentTxCount(sets.getReceiversTransactions().size());
            setExtraTxCount(sets.getSendersTransactions().size());
        }

        @Override
        public TransactionSets createTransactionSets() {
            return transactionSets;
        }
    }

    private class CorpusDataTestConfig extends TestConfig {

        public CorpusDataTestConfig(int extraTxCount, int absentTxCount, int cellCount) {
            super(0, extraTxCount, absentTxCount, 3, 8, 64, 4, cellCount);
        }

        @Override
        public TransactionSets createTransactionSets() {
            List<Transaction> randomTransactions = getRandomTransactions(getExtraTxCount() + getAbsentTxCount(), false);
            TransactionSets transactionSets = new TransactionSets();
            // As with most other tests, we just care about differences. Transactions that are both in sender's and
            // receiver's transacitons will just be added and deleted so they don't affect the result.
            transactionSets.setSendersTransactions(randomTransactions.subList(0, getExtraTxCount()));
            transactionSets.setReceiversTransactions(randomTransactions.subList(getExtraTxCount(), getExtraTxCount() + getAbsentTxCount()));
            return transactionSets;
        }
    }
}