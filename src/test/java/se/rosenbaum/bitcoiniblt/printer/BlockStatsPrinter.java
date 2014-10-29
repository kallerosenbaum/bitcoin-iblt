package se.rosenbaum.bitcoiniblt.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.chart.BarChart;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public abstract class BlockStatsPrinter {
    Logger logger = LoggerFactory.getLogger(BlockStatsPrinter.class);
    public static final String OUTPUT_FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s,%s\n";
    public static final String LOGGER_FORMAT = "{},{},{},{},{},{},{},{},{}";
    PrintWriter writer;
    private String filePrefix;
    private File tempDirectory;
    int[] category;
    int[] yValues;
    int currentDataPoint = 0;

    public BlockStatsPrinter(File tempDirectory, String filePrefix, int dataPoints) throws IOException {
        this.tempDirectory = tempDirectory;
        this.filePrefix = filePrefix;
        String fileName = filePrefix + ".csv";
        writer = new PrintWriter(new FileWriter(new File(tempDirectory, fileName)));
        writer.printf(OUTPUT_FORMAT, "txcount", "hashFunctionCount", "keySize [B]", "valueSize [B]", "keyHashSize [B]",
                "cellCount",
                "encodeTime [ms]", "decodeTime [ms]", "minIBLTSize [B]");
        category = new int[dataPoints];
        yValues = new int[dataPoints];
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

    public void close() throws IOException {
        writer.close();
        createImage();

    }

    protected void createImage(String categoryCaption,
                             String valueCaption) throws IOException {
        BarChart barChart = new BarChart(category, yValues, categoryCaption, valueCaption);

        OutputStream out = new FileOutputStream(new File(tempDirectory, filePrefix + ".png"));
        try {
            ImageIO.write(barChart.getImage(), "png", out);
        } catch (IOException e) {
            logger.error("Failed to write image.", e);
        }
        out.close();
    }

    protected abstract void createImage() throws IOException;

    protected abstract void createDataPoint(TestConfig config, BlockStatsResult result);
}
