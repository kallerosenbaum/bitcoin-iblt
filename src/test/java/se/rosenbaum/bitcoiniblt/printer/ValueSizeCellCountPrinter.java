package se.rosenbaum.bitcoiniblt.printer;

import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.File;
import java.io.IOException;

public class ValueSizeCellCountPrinter extends BlockStatsPrinter {
    public ValueSizeCellCountPrinter(File tempDirectory, String filePrefix, int dataPoints) throws IOException {
        super(tempDirectory, filePrefix, dataPoints);
    }

    @Override
    public void createDataPoint(TestConfig config, BlockStatsResult result) {
        category[currentDataPoint] = config.getValueSize();
        yValues[currentDataPoint] = config.getIbltSize();
        currentDataPoint++;
    }

    @Override
    protected void createImage() throws IOException {
        createImage("value size [B]", "Minimum IBLT size [B]");
    }
}
