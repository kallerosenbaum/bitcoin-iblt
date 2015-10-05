package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Block;
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
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

public class CorpusDataTestManual extends BlockStatsClientCoderTest {

    private CorpusData corpusStats;
    private File testFileDir;
    private File testResultDir;

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
        try {
            corpusStats.calculateStatistics();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        blockCount = corpusStats.blockCount;

        testFileDir = new File(tempDirectory, "corpustestfiles");
        testFileDir.mkdirs();
        testResultDir = new File(tempDirectory, "corpustestresults");
        testResultDir.mkdirs();
    }

    @Test
    public void testFactor1() throws IOException {
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
    public void testFromTestFiles() throws IOException {
        int cellCount = 300;

        TestFileTestConfigGenerator configGenerator = null;
        for (int factor : new int[] {1, 10, 100, 1000}) {
            FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);
            if (factor > 1) {
                cellCount = configGenerator.getCellCount() * 9;
            }
            configGenerator = new TestFileTestConfigGenerator(getFile(factor), 3, 8, 64, 4, cellCount);

            configGenerator = calculate5percent(printer, getFile(factor), configGenerator, factor);
        }
    }

    @Test
    public void testFromTestFile() throws IOException {
        int cellCount = 6000;
        int factor = 100;
        TestFileTestConfigGenerator configGenerator;

        FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);
        configGenerator = new TestFileTestConfigGenerator(getFile(factor), 3, 8, 64, 4, cellCount);

        calculate5percent(printer, getFile(factor), configGenerator, factor);
    }

    @Test
    public void testFromRealDataFile() throws IOException {
        int cellCount = 600;
        File testFile = new File(testFileDir, "test-real.txt");

        FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);
        TestFileTestConfigGenerator configGenerator = new TestFileTestConfigGenerator(testFile, 3, 8, 64, 4, cellCount);

        calculate5percent(printer, testFile, configGenerator, -1);
    }

    private TestFileTestConfigGenerator calculate5percent(FailureProbabilityPrinter printer, File testFile, TestFileTestConfigGenerator configGenerator, int factor) throws IOException {
        Interval interval = new Interval(0, configGenerator.getCellCount());
        ResultStats closestResult = null;
        TestFileTestConfigGenerator closestTestConfig = null;
        while (true) {
            ResultStats result = testFailureProbability(printer, configGenerator);
            printer.addResult(configGenerator, result);
            if (result.getFailureProbability() <= 0.05) {
                if (result.getFailureProbability() == 0.05) {
                    interval.setLow(configGenerator.getCellCount());
                }
                interval.setHigh(configGenerator.getCellCount());
                closestResult = result;
                closestTestConfig = configGenerator;
            } else {
                interval.setLow(configGenerator.getCellCount());
            }
            configGenerator = new TestFileTestConfigGenerator(testFile, configGenerator.getHashFunctionCount(),
                    configGenerator.getKeySize(), configGenerator.getValueSize(), configGenerator.getKeyHashSize(),
                    interval.nextValue(configGenerator));

            if (!interval.isInsideInterval(configGenerator.getCellCount())) {
                configGenerator.setCellCount(interval.getHigh());
                break;
            }
        }
        if (factor == -1) {
            printTestResultFile(closestTestConfig, closestResult, testFile);
        } else {
            printTestResultFile(closestTestConfig, closestResult, factor, testFile);
        }
        printer.finish();
        return closestTestConfig;
    }


    @Test
    public void testGenerateTestFileFactor1_10_100_1000() throws IOException {
        int sampleCount = 1000;

        for (int i = 0; i < 4; i++) {
            createTestFile(sampleCount, i);
        }
    }

    @Test
    public void testFindPercentilesOfExtras() throws IOException {
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
                if (intPair.unknowns <= averageExtras) {
                    countBelowEqualAverageExtras++;
                } else {
                    countMoreThanAverageExtras++;
                }
            }
        }

        //  processBlocks(CorpusData.HIGHEST_BLOCK_HASH, 720, )
   /*
    1. Collect all extras (not coinbases) for all blocks and nodes from corpus. Calculate the average extras, E, over the remaining.
    2. Calculate the average tx rate, R, over the corpus. Sum the number of transactions in all blocks and divide it with the data collection period in seconds.
    3. Now calculate the "extras per tx rate", E/R.
    4. Absents, A, is calculated from E and the ratio extras/absent
    5. Assume that E/R is constant and that the extras/absent ratio holds for all tx rates.
    */

        System.out.println("Assumed extras/absents: 1/1");

        System.out.println("Number of AU blocks: " + corpusStats.blockCount);
        System.out.println("Exact avg extras   : " + corpusStats.averageExtrasPerBlock);
        System.out.println("Ceil of extras, E  : " + averageExtras);
        System.out.println("Avg tx rate, R     : " + corpusStats.txRate);
        System.out.println("Avg E/R            : " + corpusStats.extrasPerTxRate);

        System.out.println("Count <= E         : " + countBelowEqualAverageExtras);
        System.out.println("Count >  E         : " + countMoreThanAverageExtras);
        System.out.println("Percentage below   : " + 100 * countBelowEqualAverageExtras / (countBelowEqualAverageExtras + countMoreThanAverageExtras));

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

    @Test
    public void createTestFileMimicCorpus() throws IOException {
        FileWriter fileWriter = new FileWriter(new File(testFileDir, "test-real.txt"));
        final TestFilePrinter testFilePrinter = new TestFilePrinter(fileWriter);

        final List<String> extra = new ArrayList<String>();
        final List<String> absent = new ArrayList<String>();


        corpusStats.getStats(CorpusData.Node.AU, new CorpusData.RecordHandler() {
            boolean firstBlock = true;

            public void handle(Record record) {
                if (record.type == Type.COINBASE) {
                    if (!firstBlock) {
                        while (absent.size() < extra.size()) {
                            List<Transaction> randomTransactions = getRandomTransactions(extra.size() - absent.size(), false);
                            for (Transaction randomTransaction : randomTransactions) {
                                String hashAsString = randomTransaction.getHashAsString();
                                if (!absent.contains(hashAsString)) {
                                    absent.add(hashAsString);
                                }
                            }
                        }
                        testFilePrinter.writeTransactions(extra, absent);
                        extra.clear();
                        absent.clear();
                    } else {
                        firstBlock = false;
                    }
                } else if (record.type == Type.UNKNOWN) {
                    extra.add(new Sha256Hash(record.txid).toString());
                } else if (record.type == Type.MEMPOOL_ONLY) {
                    if (absent.size() < extra.size()) {
                        // Fill up with made up absent transactions, by taking equally many from MEMPOOL_ONLY
                        // as there are extra.
                        Sha256Hash sha256Hash = new Sha256Hash(record.txid);
                        try {
                            if (getTransaction(sha256Hash) != null) {
                                absent.add(sha256Hash.toString());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });

        testFilePrinter.writeTransactions(extra, absent);
        fileWriter.close();
    }

    private int averageBlockSize() {
        int totalBlockSize = 0;
        Block block = getBlock(new Sha256Hash(MAINNET_BLOCK));
        for (int i = 1; i < corpusStats.blockCount; i++) {
            totalBlockSize += block.getOptimalEncodingMessageSize();
            block = getBlock(block.getPrevBlockHash());
        }
        return totalBlockSize / corpusStats.blockCount;
    }

    private void printTestResultFile(TestConfig testConfig, ResultStats result, File inputFile) throws IOException {
        File resultFile = getResultFile("test-result-real");
        PrintWriter out = new PrintWriter(new FileWriter(resultFile));
        out.println("Input file                    : " + inputFile.getName());
        out.println("Average block size [Bytes]    : " + averageBlockSize());
        out.println("Average tx count per block    : " + (corpusStats.txCount / corpusStats.blockCount));
        printCommon(testConfig, result, out);
        out.flush();
        out.close();
    }

    private void printTestResultFile(TestConfig testConfig, ResultStats result, int factor, File inputFile) throws IOException {
        File resultFile = getResultFile("test-result-factor-" + factor);
        PrintWriter out = new PrintWriter(new FileWriter(resultFile));
        out.println("Input file                    : " + inputFile.getName());
        out.println("Estimated block size [Bytes]  : " + averageBlockSize() * factor);
        out.println("Estimated tx count per block  : " + (corpusStats.txCount / corpusStats.blockCount) * factor);
        out.println("Extra tx                      : " + testConfig.getExtraTxCount());
        out.println("Absent tx                     : " + testConfig.getAbsentTxCount());
        printCommon(testConfig, result, out);
        out.flush();
        out.close();
    }

    private void printCommon(TestConfig testConfig, ResultStats result, PrintWriter out) {
        out.println("Sample count                  : " + (result.getFailures() + result.getSuccesses()));
        out.println("IBLT size for 5% probability  : " + testConfig.getIbltSize());
        out.println("Cell count for 5% probability : " + testConfig.getCellCount());
        out.println("Hash functions                : " + testConfig.getHashFunctionCount());
        out.println("Key size                      : " + testConfig.getKeySize());
        out.println("Value size                    : " + testConfig.getValueSize());
        out.println("KeyHashSize                   : " + testConfig.getKeyHashSize());
    }

    private File getResultFile(String prefix) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return new File(testResultDir, prefix + "-" + dateFormat.format(new Date()) + ".txt");
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
            writer.write("extra:");
            writeTransactions(sets.getSendersTransactions());
            writer.write("absent:");
            writeTransactions(sets.getReceiversTransactions());
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

        private void writeTransactions(List<String> extra, List<String> absent) {
            try {
                writer.write("extra:");
                writeTransactionStrings(extra);
                writer.write("absent:");
                writeTransactionStrings(absent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void writeTransactionStrings(List<String> transactions) throws IOException {
            boolean first = true;
            for (String transaction : transactions) {
                if (!first) {
                    writer.write(",");
                }
                first = false;
                writer.write(transaction);
            }
            writer.write("\n");
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
                line = line.substring("extra:".length());
                addTransactionsToList(line, transactionSets.getSendersTransactions());

                line = fileReader.readLine();
                line = line.substring("absent:".length());
                addTransactionsToList(line, transactionSets.getReceiversTransactions());
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