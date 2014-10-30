package se.rosenbaum.bitcoiniblt.printer;

import se.rosenbaum.bitcoiniblt.util.ResultStats;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.File;
import java.io.IOException;

public class CellCountFailureProbabilityPrinter extends BlockStatsPrinter {
    private final int[] category;
    private final double[] yValues;
    private static final String FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s,%s\n";

    public CellCountFailureProbabilityPrinter(File tempDirectory, int dataPoints) throws IOException {
        super(tempDirectory);
        category = new int[dataPoints];
        yValues = new double[dataPoints];
        writer.printf(FORMAT, "txcount", "hashFunctionCount", "keySize [B]", "valueSize [B]", "keyHashSize [B]",
                "cellCount", "failureCount", "successCount", "failureProbability");
    }

    public void addResult(TestConfig config, ResultStats resultStats) {
        writer.printf(FORMAT, config.getTxCount(), config.getHashFunctionCount(), config.getKeySize(),
                config.getValueSize(), config.getKeyHashSize(), config.getCellCount(),
                resultStats.getFailures(), resultStats.getSuccesses(),
                (double)resultStats.getFailures() / (double)(resultStats.getFailures() + resultStats.getSuccesses()));
        writer.flush();
    }

    @Override
    protected String filePrefix() {
        return "cellCount-failureProbability-Stats";
    }
}
