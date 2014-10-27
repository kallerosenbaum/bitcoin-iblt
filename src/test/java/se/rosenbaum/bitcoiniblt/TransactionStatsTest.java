package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.SimpleHistogramBin;
import org.jfree.data.statistics.SimpleHistogramDataset;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.Arrays;

import static org.junit.Assert.fail;

public class TransactionStatsTest extends ClientCoderTest {
    private static final Logger logger = LoggerFactory.getLogger(TransactionStatsTest.class);
    public static final int TRANSACTION_COUNT = 10000;
    public static final String FILE_PREFIX = "transactionStats";

    @Test
    public void testGetTransactionStats() throws IOException {
        File csvFile = new File(tempDirectory, FILE_PREFIX + ".csv");
        Writer writer = new FileWriter(csvFile);

        SizePrintingProcessor processor = new SizePrintingProcessor(writer);
        processTransactions(MAINNET_BLOCK, TRANSACTION_COUNT, processor);
        writer.close();
        printPercentiles(processor.transactionSizes);
    }

    private void printPercentiles(int[] data) throws IOException {
        Arrays.sort(data);
        File file = new File(tempDirectory, FILE_PREFIX + "_percentiles.csv");
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        for (int i = 0; i < 10; i++) {
            int percentile = 10 * (i + 1);
            int percentileIndex = (data.length / 10) * (i+1) - 1;
            writer.printf("percentile %s: %s\n", percentile, data[percentileIndex]);
        }
        writer.close();
    }

    private void createHistogram(int[] values) throws IOException {
        HistogramDataset dataset = new HistogramDataset();

        //dataset.setAdjustForBinSize(true);
        //dataset.setType
/*        for (int i = 0; i < 1000; i += 50) {
            SimpleHistogramBin histogramBin = new SimpleHistogramBin(i, i+50, true, false);
            dataset.addBin(histogramBin);
        }
        for (int i = 1000; i < 10000; i += 1000) {
            SimpleHistogramBin histogramBin = new SimpleHistogramBin(i, i+1000, true, false);
            dataset.addBin(histogramBin);
        }
  */
       // SimpleHistogramBin histogramBin = new SimpleHistogramBin(10000, 100000, true, true);
       // dataset.addBin(histogramBin);
        /*
        for (int i = 10000; i < 100000; i += 10000) {
            SimpleHistogramBin histogramBin = new SimpleHistogramBin(i, i+10000, true, false);
            dataset.addBin(histogramBin);
        }
        */
        /*
        for (int value : values) {
            dataset.addObservation(value, false);
        } */
        double[] dValues = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            dValues[i] = values[i];
        }
        dataset.addSeries("size", dValues, 100, 0, 4000);

        JFreeChart chart = ChartFactory.createHistogram("Transaction sizes", "size [B]", "Frequency", dataset,
            PlotOrientation.VERTICAL,
            false, false, false);


        OutputStream out = new FileOutputStream(new File(tempDirectory, FILE_PREFIX + ".png"));
        try {
            ImageIO.write(chart.createBufferedImage(600,800), "png", out);
        } catch (IOException e) {
            logger.error("Failed to write image.", e);
        }
        out.close();

    }

    private static class SizePrintingProcessor implements TransactionProcessor {
        private Writer writer;
        int[] transactionSizes = new int[TRANSACTION_COUNT];
        int i = 0;

        public SizePrintingProcessor(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void process(Transaction transaction) {
            try {
                int length = transaction.bitcoinSerialize().length;
                writer.write(length + "," + transaction.getInputs().size() +
                        "," + transaction.getOutputs().size() + "," + transaction.getHash().toString() + "\n");
                transactionSizes[i++] = length;
            } catch (IOException e) {
                fail();
            }
        }
    }

    public NetworkParameters getParams() {
        return MainNetParams.get();
    }
}
