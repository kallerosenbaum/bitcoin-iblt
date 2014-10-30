package se.rosenbaum.bitcoiniblt.printer;

import se.rosenbaum.bitcoiniblt.chart.BarChart;
import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * User: kalle
 * Date: 10/30/14 7:19 PM
 */
public abstract class IBLTSizeBlockStatsPrinter extends BlockStatsPrinter {
    public static final String OUTPUT_FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s,%s\n";
    public static final String LOGGER_FORMAT = "{},{},{},{},{},{},{},{},{}";
    int[] category;
    int[] yValues;
    int currentDataPoint = 0;

    protected IBLTSizeBlockStatsPrinter(File tempDirectory, int dataPoints) throws IOException {
        super(tempDirectory);
        printHeader();
        category = new int[dataPoints];
        yValues = new int[dataPoints];

    }

    private void printHeader() {
        writer.printf(OUTPUT_FORMAT, "txcount", "hashFunctionCount", "keySize [B]", "valueSize [B]", "keyHashSize [B]",
                "cellCount", "encodeTime [ms]", "decodeTime [ms]", "minIBLTSize [B]");
    }

    public void logResult(TestConfig config, BlockStatsResult result) {
        logger.info(LOGGER_FORMAT, config.getTxCount(), config.getHashFunctionCount(), config.getKeySize(),
                config.getValueSize(),
                config.getKeyHashSize(), config.getCellCount(),
                result.getEncodingTime(), result.getDecodingTime(), result.isSuccess() ? "success" : "fail");
    }

    public void addResult(TestConfig config, BlockStatsResult result) {
        writer.printf(OUTPUT_FORMAT, config.getTxCount(), config.getHashFunctionCount(), config.getKeySize(),
                config.getValueSize(), config.getKeyHashSize(),
                config.getCellCount(), result.getEncodingTime(), result.getDecodingTime(), config.getIbltSize());
        writer.flush();
        createDataPoint(config, result);
    }

    protected void createImage(String categoryCaption,
                             String valueCaption) throws IOException {
        BarChart barChart = new BarChart(category, yValues, categoryCaption, valueCaption);

        OutputStream out = new FileOutputStream(getFile(".png"));
        try {
            ImageIO.write(barChart.getImage(), "png", out);
        } catch (IOException e) {
            logger.error("Failed to write image.", e);
        }
        out.close();
    }

    @Override
    public void finish() throws IOException {
        super.finish();
        createImage();
    }

    protected abstract void createDataPoint(TestConfig config, BlockStatsResult result);

    protected abstract void createImage() throws IOException;
}
