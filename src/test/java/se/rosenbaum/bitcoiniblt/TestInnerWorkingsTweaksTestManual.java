package se.rosenbaum.bitcoiniblt;

import org.junit.Test;
import se.rosenbaum.bitcoiniblt.printer.CellCountVSFailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.printer.FailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.util.AggregateResultStats;
import se.rosenbaum.bitcoiniblt.util.Interval;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.IOException;

public class TestInnerWorkingsTweaksTestManual extends BlockStatsClientCoderTest {

    @Test
    public void testCellSelfTest() throws IOException {
        System.out.println("keyHashSize, fp");
        for (int i = 0; i < 100; i++) {
            for (int keyHashSumSize : new int[]{4, 0}) {
                TestConfig config = new RandomTransactionsTestConfig(256, 256, 256, 3, 8, 64, keyHashSumSize, 4701, false) {
                    @Override
                    public boolean assertTransactionListCorrect() {
                        return false;
                    }
                };

                AggregateResultStats result = testFailureProbability(null, config, 100);

                System.out.printf("%d, %f%n", keyHashSumSize, result.getFailureProbability());
            }
        }
    }

}


