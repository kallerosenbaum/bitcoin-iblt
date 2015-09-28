package se.rosenbaum.bitcoiniblt;

import org.junit.Test;
import se.rosenbaum.bitcoiniblt.printer.DiffCountVSFailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.util.ResultStats;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.IOException;

public class DiffCountVSFailureProbabilityTestManual extends BlockStatsClientCoderTest {

    @Test
    public void testRustyRussell() throws IOException {
        TestConfig config = new RandomTransactionsTestConfig(0, 0, 0, 4, 8, 8, 4, 52428, true);

        DiffCountVSFailureProbabilityPrinter printer = new DiffCountVSFailureProbabilityPrinter(tempDirectory);

        int[] tests = new int[]{700, 800, 900, 1000, 1100, 1200, 1300};
        for (int i = 0; i < tests.length; i++) {
            int diffs = tests[i];
            int halfDiffs = diffs / 2;
            config.setTxCount(halfDiffs);
            config.setExtraTxCount(halfDiffs);
            config.setAbsentTxCount(halfDiffs);

//            ResultStats result = testFailureProbability(printer, config, 100);
            ResultStats result = testFailureProbability(printer, config, 1000);
            printer.addResult(config, result);
        }
        printer.finish();
    }

    @Test
    public void testDiffCountVSFailureProbability() throws IOException {
        TestConfig config = new RandomTransactionsTestConfig(0, 0, 0, 3, 8, 64, 4, 3000, true);

        DiffCountVSFailureProbabilityPrinter printer = new DiffCountVSFailureProbabilityPrinter(tempDirectory);
        for (int i = 3; i <= 5; i++) {
            config.setHashFunctionCount(i);
            testDiffCountVSFailureProbability(config, printer);
        }
        printer.finish();
    }

    private void testDiffCountVSFailureProbability(TestConfig config, DiffCountVSFailureProbabilityPrinter printer) throws IOException {
        int[] tests = new int[]{100, 200, 300, 400, 500, 600, 700, 800};
        for (int i = 0; i < tests.length; i++) {
            int diffs = tests[i];
            int halfDiffs = diffs / 2;
            config.setTxCount(halfDiffs);
            config.setExtraTxCount(halfDiffs);
            config.setAbsentTxCount(halfDiffs);

//            ResultStats result = testFailureProbability(printer, config, 100);
            ResultStats result = testFailureProbability(printer, config, i < 1 ? 100000 : i < 3 ? 10000 : 1000);
            printer.addResult(config, result);
        }
    }

}


