package se.rosenbaum.bitcoiniblt.printer;

import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.File;
import java.io.IOException;

public class ValueSizeCellCountPrinter extends IBLTSizeBlockStatsPrinter {
    public ValueSizeCellCountPrinter(File tempDirectory, int dataPoints) throws IOException {
        super(tempDirectory, dataPoints);
    }

    @Override
    protected void createDataPoint(TestConfig config, BlockStatsResult result) {
        category[currentDataPoint] = config.getValueSize();
        yValues[currentDataPoint] = config.getIbltSize();
        currentDataPoint++;
    }

    @Override
    protected void createImage() throws IOException {
        createImage("value size [B]", "Minimum IBLT size [B]");
    }

    @Override
    protected String filePrefix() {
        return "valueSize-cellCount-Stats";
    }

}
