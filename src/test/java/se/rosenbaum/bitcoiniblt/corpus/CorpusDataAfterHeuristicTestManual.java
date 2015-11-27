package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;
import org.junit.Before;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.printer.FailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.printer.IBLTSizeVsFailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.util.AggregateResultStats;
import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CorpusDataAfterHeuristicTestManual extends CorpusDataTestManual {
    public static final String COMMON_HEADERS = " raw-blocks success failure fail-prob sent-bytes%n";
    public static final String COMMON_PATTERN = "%11d %7d %7d %9f %10d%n";
    public static final String RUSTY_HEADERS = "init-tx factor" + COMMON_HEADERS;
    public static final String RUSTY_PATTERN = "%7d %6.2f";
    public static final String MODIFIED_RUSTY_HEADERS = "init-tx" + COMMON_HEADERS;
    public static final String MODIFIED_RUSTY_PATTERN = "%7d";


    private File fullCorpusWithHintsNoWeak;
    private File fullCorpusWithHintsWeak;
    private File fullCorpusWithHintsWeakOnlyStrong;

    @Before
    public void setup() {
        fullCorpusWithHintsNoWeak = new File(testProps.getProperty("rustyiblt.corpus.with.hints.noweak"));
        fullCorpusWithHintsWeak = new File(testProps.getProperty("rustyiblt.corpus.with.hints.weak"));
        fullCorpusWithHintsWeakOnlyStrong = new File(testProps.getProperty("rustyiblt.corpus.with.hints.weak.only.strong.output"));
    }

    @Test
    public void testFromFullCorpusTestWithHints() throws Exception
    {
        int cellCount = 501;
        FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);
        FullCorpusWithHintsTestConfigGenerator configGenerator = new FullCorpusWithHintsTestConfigGenerator(fullCorpusWithHintsNoWeak, cellCount, this);

        calculateSizeFromTargetProbability(printer, fullCorpusWithHintsNoWeak, configGenerator, -1, 0.05);
    }

    @Test
    public void testDifferentCalculateSizeHeuristics() throws Exception {
        print(RUSTY_HEADERS);
        for (int i = 1; i < 5; i++) {
            for (double factor = 1.0; factor < 1.61; factor += 0.05) {
                testRustysSizeCalculator(i, factor);
            }
        }
        print(MODIFIED_RUSTY_HEADERS);
        for (int i = 1; i < 5; i++) {
            testModifiedRustysSizeCalculator(i);
        }
    }

    @Test
    public void testFromFullCorpusTestWithHintsDynamicMultipleInitialTx() throws Exception {
        print(MODIFIED_RUSTY_HEADERS);
        for (int i = 1; i < 5; i++) {
            testModifiedRustysSizeCalculator(i);
        }
    }

    @Test
    public void testFromFullCorpusTestWithHintsDynamic4InitialTx() throws Exception {
        print(MODIFIED_RUSTY_HEADERS);
        testModifiedRustysSizeCalculator(4);
    }

    @Test
    public void testFromFullCorpusTestWithHintsDynamic4InitialTxRusty() throws Exception {
        print(RUSTY_HEADERS);
        testRustysSizeCalculator(2, 1.35);
    }

    @Test
    public void testFromFullCorpusTestWithHintsDynamic4InitialTxRustyWithWeak() throws Exception {
        RustysSizeCalculator sizeCalculator = new RustysSizeCalculator(4, this, 1.3);
        runFullCorpusTestWithHintsDynamic(sizeCalculator, fullCorpusWithHintsWeak, true, false);
    }

    @Test
    public void testFromFullCorpusTestWithHintsDynamic4InitialTxRustyWithWeakOnlyStrong() throws Exception {
        print(RUSTY_HEADERS);
        for (int i = 1; i < 5; i++) {
            for (double factor = 1.0; factor <= 1.61; factor += 0.05) {
                print(RUSTY_PATTERN, i, factor);
                final RustysSizeCalculator sizeCalculator = new RustysSizeCalculator(i, this, factor);
                runFullCorpusTestWithHintsDynamic(sizeCalculator, fullCorpusWithHintsWeakOnlyStrong, false, false);
            }
        }
    }

    private void testRustysSizeCalculator(int initialTxCount, double extraFactor) throws Exception
    {
        final RustysSizeCalculator sizeCalculator = new RustysSizeCalculator(initialTxCount, this, extraFactor);
        print(RUSTY_PATTERN, initialTxCount, extraFactor);
        runFullCorpusTestWithHintsDynamic(sizeCalculator, fullCorpusWithHintsNoWeak, false, false);
    }

    private void testModifiedRustysSizeCalculator(int initialTxCount) throws Exception
    {
        final ModifiedRustysSizeCalculator sizeCalculator = new ModifiedRustysSizeCalculator(initialTxCount, this);
        print(MODIFIED_RUSTY_HEADERS);
        print(MODIFIED_RUSTY_PATTERN, initialTxCount);
        runFullCorpusTestWithHintsDynamic(sizeCalculator, fullCorpusWithHintsNoWeak, false, false);
    }

    private void runFullCorpusTestWithHintsDynamic(final SizeCalculator sizeCalculator, File inputFile, boolean fileIncludesWeak, boolean processWeak) throws Exception {
        final FullCorpusWithHintsDynamicCellCountTestConfigGenerator configGenerator =
                new FullCorpusWithHintsDynamicCellCountTestConfigGenerator(sizeCalculator, inputFile, this, fileIncludesWeak, processWeak);
//        print("height  from    to unknown uk-bytes  cells bl-only mp-only success\n");
        final AggregateResultStats resultStats = new AggregateResultStats();
        TestListener listener = new TestListener() {
            public void testPerformed(TestConfig config, BlockStatsResult result) {
                resultStats.addSample(result);

//                if (!result.isSuccess()) {
//                    print("%6d%6s%6s%8d%9d%7d%8d%8d%8b%n", configGenerator.currentTransfer.ibltBlock.getHeight(),
//                            configGenerator.currentTransfer.ibltBlock.ibltData.getNodeName(),
//                            configGenerator.currentTransfer.receiverTxGuessData.getNodeName(),
//                            sizeCalculator.getUnknowns(),
//                            sizeCalculator.getUnknownBytes(),
//                            config.getCellCount(), config.getExtraTxCount(), config.getAbsentTxCount(), result.isSuccess());
//                }
            }
        };
        testFailureProbabilityForConfigGenerator(configGenerator, listener);
        double failureProbability = ((double)resultStats.getFailures()) / (resultStats.getFailures() + resultStats.getSuccesses() + configGenerator.getRawBlocks());
        print(COMMON_PATTERN,
                configGenerator.getRawBlocks(),
                resultStats.getSuccesses(), resultStats.getFailures(), failureProbability,
                configGenerator.getTotalBytesSent());
    }

    // Info:
    // Using iblt-encode on the same input file as this and then iblt-decode yields the exact same result rusty. 125 failures of 2112.
    @Test
    public void testRustys336Buckets() throws Exception {
        int cellCount = 336; // Rustys result (but with 339 instead of 337 because of subtables). I get 0.060606, 0.061553, 0.062973, 0.061553
                             // With rusty's fixes of bad mempools and 336 cells I get: 0.049468,0.051318, 0.050393, 0.050393
        //int cellCount = 453;    // This is my test result for target failure probability 125.0/2112 (Rusty's failure probability). I get 0.059659, 0.059659,0.059659
                               // With rusty's fixes of bad mempools I get: 0.047157, 0.050393, 0.050855, 0.049931
        FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);
        FullCorpusWithHintsTestConfigGenerator configGenerator = new FullCorpusWithHintsTestConfigGenerator(fullCorpusWithHintsNoWeak, cellCount, this);

        AggregateResultStats result = testFailureProbabilityForConfigGenerator(printer, configGenerator);

        print("Successes: %d, Failures: %d, Failure probability: %f%n", result.getSuccesses(), result.getFailures(), result.getFailureProbability());

    }

    @Test
    public void testCalculateTotalOverhead() throws Exception
    {
        IBLTBlockStream blockStream = new IBLTBlockStream(fullCorpusWithHintsNoWeak, this);
        List<IBLTBlockStream.IBLTBlockTransfer> blockTransfers = blockStream.getNextBlockTransfers();
        int totalOverhead = 0;
        int numberOfTransfers = 0;
        int numberOfBigTransfers = 0;
        int numberOfBlockOnly = 0;
        int numberOfMempoolOnly = 0;
        String pattern = "%6d%6s%6s%6d%8d%6d%8d%8d%n";
        print("height  from    to block mempool guess bl-only mp-only%n");
        while (blockTransfers != null)
        {
            for (IBLTBlockStream.IBLTBlockTransfer blockTransfer : blockTransfers)
            {
                IBLTBlockStream.IBLTBlock ibltBlock = blockTransfer.ibltBlock;
                Set<Sha256Hash> blockOnly = blockTransfer.getBlockOnly();
                int blockOnlyCount = blockOnly.size();
                Set<Sha256Hash> mempoolOnly = blockTransfer.getMempoolOnly();
                int mempoolOnlyCount = mempoolOnly.size();
                numberOfBlockOnly += blockOnlyCount;
                numberOfMempoolOnly += mempoolOnlyCount;
//				if (mempoolOnlyCount + blockOnlyCount > 20) {
//                    print(pattern, ibltBlock.getHeight(), ibltBlock.ibltData.getNodeName(), blockTransfer.receiverTxGuessData.getNodeName(),
//                            blockTransfer.ibltBlock.ibltData.transactions.size(), blockTransfer.ibltBlock.senderMempool.transactions.size(),
//                            blockTransfer.receiverTxGuessData.transactions.size(), blockOnlyCount, mempoolOnlyCount);
//                    numberOfBigTransfers++;
//				}
                totalOverhead += blockTransfer.getOverhead();
                numberOfTransfers++;

            }
            blockTransfers = blockStream.getNextBlockTransfers();
        }
        print("Total overhead = %d%n", totalOverhead);
        print("Total transfers = %d%n", numberOfTransfers);
        print("Big transfers = %d%n", numberOfBigTransfers);
        print("Mempool-only total = %d%n", numberOfMempoolOnly);
        print("Block-only total = %d%n", numberOfBlockOnly);
    }

    @Test
    public void testInfiniteLoop() throws IOException {
        String senderTx = "882e5a73ff3744c59dcbe1286aea95d6742b6264b492a5b62450c420f68a7f80";   // Stor 1110 bytes
        String receiverTx = "b31d8ab7b0f7afff6f780be0ddebf64b146986ab3c5f64322acf18e4970e183e"; // Liten 369 bytes
        int cellCount = 45;

        TransactionSets sets = new TransactionSets();
        sets.setSendersTransactions(Collections.singletonList(getTransaction(new Sha256Hash(senderTx))));
        sets.setReceiversTransactions(Collections.singletonList(getTransaction(new Sha256Hash(receiverTx))));

        SpecificSaltTestConfig testConfig = new SpecificSaltTestConfig(sets, 3, 8, 64, 0, cellCount);

        long salt = 1971;

        while (true) {
            testConfig.setSalt(salt);
            BlockStatsResult blockStatsResult = testBlockStats(testConfig);

            print("%d Success: %s%n", salt, blockStatsResult.isSuccess());
            salt++;
        }
    }

    private void print(String message, Object... params)
    {
        System.out.printf(message, params);
    }

    private class SpecificSaltTestConfig extends TransactionSetsTestConfig {
        long salt = 0;
        SpecificSaltTestConfig(TransactionSets sets, int hashFunctionsCount, int keySize, int valueSize,
                               int keyHashSize, int cellCount) {
            super(sets, hashFunctionsCount, keySize, valueSize, keyHashSize, cellCount);
        }

        private void setSalt(long salt) {
            this.salt = salt;
        }

        @Override
        public byte[] getSalt() {
            byte[] saltBytes = new byte[32];
            ByteBuffer byteBuffer = ByteBuffer.wrap(saltBytes);
            byteBuffer.putLong(salt);
            return saltBytes;
        }
    }

}
