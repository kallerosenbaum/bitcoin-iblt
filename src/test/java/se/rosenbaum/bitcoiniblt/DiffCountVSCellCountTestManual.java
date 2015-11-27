package se.rosenbaum.bitcoiniblt;

import org.junit.Test;
import se.rosenbaum.bitcoiniblt.printer.CellCountVSFailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.printer.FailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.printer.IBLTSizeBlockStatsPrinter;
import se.rosenbaum.bitcoiniblt.printer.ValueSizeCellCountPrinter;
import se.rosenbaum.bitcoiniblt.util.AggregateResultStats;
import se.rosenbaum.bitcoiniblt.util.Interval;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.File;
import java.io.IOException;

public class DiffCountVSCellCountTestManual extends BlockStatsClientCoderTest {

    @Test
    public void testFind1pctLine64() throws IOException {
        TestConfig config = new RandomTransactionsTestConfig(0, 0, 0, 3, 8, 64, 4, 16, true);

        double[] failureProbabilities = new double[] {0.05, 0.02, 0.01, 0.005, 0.001};
        int[] samplesNeeded = new int[] {2000, 5000, 10000, 20000, 100000};

        CellCountVSFailureProbabilityPrinter printer = new CellCountVSFailureProbabilityPrinter(tempDirectory);
        for (int i = 0; i < 4; i++) {
            int diffs = 2*(int)Math.pow(2, i);
            int halfDiffs = diffs / 2;
            config.setTxCount(halfDiffs);
            config.setExtraTxCount(halfDiffs);
            config.setAbsentTxCount(halfDiffs);

            int[] cellCountEstimates = findRoughEstimate(printer, config);
            int min = cellCountEstimates[0];
            int max = cellCountEstimates[1];

            int step = (max - min) / 4;

            for (int j = 0; j < 7; j++) {
                config.setCellCountMultiple(min + step * j);
                step = (int)(step * 1.4);
                AggregateResultStats result = testFailureProbability(printer, config, j > 3 ? 100000 : 10000);
                printer.addResult(config, result);
            }
        }
        printer.finish();
    }

    @Test
    public void testRipped() throws IOException {


        TestConfig config = new RandomTransactionsTestConfig(0, 0, 0, 3, 8, 64, 4, 10000, true);

        double[] failureProbabilities = new double[] {0.001, 0.005, 0.01, 0.02, 0.05};
        int[] samplesNeeded = new int[] {100000, 20000, 10000, 5000, 2000};

        int numberOfDiffDoublings = 4;
        TestConfig[][] configs = new TestConfig[numberOfDiffDoublings][failureProbabilities.length];

        for (int i = 0; i < numberOfDiffDoublings; i++) { // For each diff count biggest first
            int diffs = 2*(int)Math.pow(2, numberOfDiffDoublings - i);
            int halfDiffs = diffs / 2;
            config.setTxCount(halfDiffs);
            config.setExtraTxCount(halfDiffs);
            config.setAbsentTxCount(halfDiffs);

            int[] cellCountEstimates = findRoughEstimate(null, config);
            int initialCellCount = cellCountEstimates[1];

            Interval interval = new Interval(0, initialCellCount);
            TestConfig closestTestConfig = null;

            for (int j = 0; j < failureProbabilities.length; j++) { // For each failure probability, hardest first
                double failureProbability = failureProbabilities[j];
                while (true) {
                    AggregateResultStats result = testFailureProbability(null, config, samplesNeeded[j]/20);
                    if (result.getFailureProbability() <= failureProbability) {
                        if (result.getFailureProbability() == failureProbability) {
                            interval.setLow(config.getCellCount());
                        }
                        interval.setHigh(config.getCellCount());
                        closestTestConfig = config;
                    } else {
                        interval.setLow(config.getCellCount());
                    }
                    config = new RandomTransactionsTestConfig(config);
                    config.setCellCount(interval.nextValue(config));

                    if (!interval.isInsideInterval(config.getCellCount())) {
                        configs[i][j] = closestTestConfig;
                        System.out.println("diffs " + diffs + ", failure probability "  + failureProbability + " size " + configs[i][j].getIbltSize());
                        break;
                    }
                }
            }
        }
    }

    // TODO: hashFunctionCount vs Failureprobability --> select best hashFunctionCount
    // TODO: possibly redo testFind1pctLine64 and testFind1pctLine270 with new hashFunctionCount
    // TODO: Do a diffCount vs Failure probability.
    // TODO: Compare 64 bytes value size with 270 bytes side by side


    private int[] findRoughEstimate(FailureProbabilityPrinter printer, TestConfig testConfig) throws IOException {
        int extras = testConfig.getExtraTxCount();
        int absents = testConfig.getAbsentTxCount();

        // valueSize
        int valueSize = testConfig.getValueSize();
        int avgCellCountPerTx = (200 / valueSize) + 1;

        int cellCount = 3 * (absents / avgCellCountPerTx + extras);

        testConfig.setCellCountMultiple(cellCount);
        AggregateResultStats resultHighP = testFailureProbability(printer, testConfig, 10);
        while (resultHighP.getFailureProbability() < 1) {
            testConfig.setCellCountMultiple(testConfig.getCellCount() / 2);
            resultHighP = testFailureProbability(printer, testConfig, 10);
        }
        while (resultHighP.getFailureProbability() == 1) {
            testConfig.setCellCountMultiple((int) (testConfig.getCellCount() + 50));
            resultHighP = testFailureProbability(printer, testConfig, 10);
        }
        TestConfig highConfig = new RandomTransactionsTestConfig(testConfig);

        AggregateResultStats resultLowP = testFailureProbability(printer, testConfig, 10);
        while (resultLowP.getFailureProbability() > 0) {
            testConfig.setCellCountMultiple(testConfig.getCellCount() + 50);
            resultLowP = testFailureProbability(printer, testConfig, 10);
        }
        TestConfig lowConfig = new RandomTransactionsTestConfig(testConfig);

        System.out.println("HighConfig: " + highConfig.getCellCount());
        System.out.println("LowConfig: " + lowConfig.getCellCount());
        return new int[] {highConfig.getCellCount(), lowConfig.getCellCount()};


    }

    private void testFind1pctLine(int valueSize) throws IOException {
        TestConfig config = new RandomTransactionsTestConfig(0, 0, 0, 3, 8, valueSize, 4, 16, true);

        int[] tests = new int[]{32, 64, 128, 256, 512, 1024};

        CellCountVSFailureProbabilityPrinter printer = new CellCountVSFailureProbabilityPrinter(tempDirectory);
        for (int i = 0; i < tests.length; i++) {
            int diffs = tests[i];
            int halfDiffs = diffs / 2;
            config.setTxCount(halfDiffs);
            config.setExtraTxCount(halfDiffs);
            config.setAbsentTxCount(halfDiffs);

            int[] cellCountEstimates = findRoughEstimate(printer, config);
            int min = cellCountEstimates[0];
            int max = cellCountEstimates[1];

            int step = (max - min) / 4;

            for (int j = 0; j < 7; j++) {
                config.setCellCountMultiple(min + step * j);
                step = (int)(step * 1.4);
                AggregateResultStats result = testFailureProbability(printer, config, j > 3 ? 100000 : 10000);
                printer.addResult(config, result);
            }
        }
        printer.finish();
    }
}


