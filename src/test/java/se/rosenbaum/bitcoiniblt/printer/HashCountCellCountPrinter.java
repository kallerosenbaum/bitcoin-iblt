package se.rosenbaum.bitcoiniblt.printer;

import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.File;
import java.io.IOException;

public class HashCountCellCountPrinter extends IBLTSizeBlockStatsPrinter {
    public HashCountCellCountPrinter(File tempDirectory, int dataPoints) throws IOException {
        super(tempDirectory, dataPoints);
    }

    @Override
    protected void createDataPoint(TestConfig config, BlockStatsResult result) {
        category[currentDataPoint] = config.getHashFunctionCount();
        yValues[currentDataPoint] = config.getIbltSize();
        currentDataPoint++;
    }

    @Override
    protected void createImage() throws IOException {
        createImage("hash function count", "Minimum IBLT size [bytes]", "Minimum decodable IBLT");
    }

    @Override
    protected String filePrefix() {
        return "hfCount-cellCount-Stats";
    }
}
