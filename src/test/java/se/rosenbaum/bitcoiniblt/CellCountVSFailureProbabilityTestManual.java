package se.rosenbaum.bitcoiniblt;

import org.junit.Test;
import se.rosenbaum.bitcoiniblt.printer.CellCountVSFailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.printer.FailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.ResultStats;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.IOException;

public class CellCountVSFailureProbabilityTestManual extends BlockStatsClientCoderTest {

    @Test
    public void testFind1pctLine270() throws IOException {
        testFind1pctLine(270);
    }

    @Test
    public void testFind1pctLine64() throws IOException {
        testFind1pctLine(64);
    }

    // TODO: hashFunctionCount vs Failureprobability --> select best hashFunctionCount
    // TODO: possibly redo testFind1pctLine64 and testFind1pctLine270 with new hashFunctionCount
    // TODO: Do a diffCount vs Failure probability.
    // TODO: Compare 64 bytes value size with 270 bytes side by side


    private int[] findRoughEstimate(FailureProbabilityPrinter printer, TestConfig testConfig) throws IOException {
        int extras = testConfig.getExtraTxCount();
        int absents = testConfig.getAbsentTxCount();
        int diffs = extras + absents;

        // valueSize
        int valueSize = testConfig.getValueSize();
        int avgCellCountPerTx = (200 / valueSize) + 1;

        int cellCount = 3 * (absents / avgCellCountPerTx + extras);

        testConfig.setCellCountMultiple(cellCount);
        ResultStats resultHighP = testFailureProbability(printer, testConfig, 10);
        while (resultHighP.getFailureProbability() < 1) {
            testConfig.setCellCountMultiple(testConfig.getCellCount() / 2);
            resultHighP = testFailureProbability(printer, testConfig, 10);
        }
        while (resultHighP.getFailureProbability() == 1) {
            testConfig.setCellCountMultiple((int) (testConfig.getCellCount() + 50));
            resultHighP = testFailureProbability(printer, testConfig, 10);
        }
        TestConfig highConfig = new TestConfig(testConfig);

        ResultStats resultLowP = testFailureProbability(printer, testConfig, 10);
        while (resultLowP.getFailureProbability() > 0) {
            testConfig.setCellCountMultiple(testConfig.getCellCount() + 50);
            resultLowP = testFailureProbability(printer, testConfig, 10);
        }
        TestConfig lowConfig = new TestConfig(testConfig);

        System.out.println("HighConfig: " + highConfig.getCellCount());
        System.out.println("LowConfig: " + lowConfig.getCellCount());
        return new int[] {highConfig.getCellCount(), lowConfig.getCellCount()};


    }

    private void testFind1pctLine(int valueSize) throws IOException {
        TestConfig config = new TestConfig(0, 0, 0, 3, 8, valueSize, 4, 16, true);

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
                ResultStats result = testFailureProbability(printer, config, j > 3 ? 100000 : 10000);
                printer.addResult(config, result);
            }
        }
        printer.finish();
    }

}


