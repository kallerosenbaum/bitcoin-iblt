package se.rosenbaum.bitcoiniblt.printer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.util.ResultStats;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public abstract class FailureProbabilityPrinter extends BlockStatsPrinter {
    private static final Logger logger = LoggerFactory.getLogger(FailureProbabilityPrinter.class);
    private static final String FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%.4f\n";
    private static final int FILE_FORMAT_VERSION = 2;

    public FailureProbabilityPrinter(File tempDirectory) throws IOException {
        super(tempDirectory);
        writer.printf("version " + FILE_FORMAT_VERSION + "\n");
        writer.printf("blockTxCount,extraCount, absentCount, hashFunctionCount,keySize [B],valueSize [B]," +
                "keyHashSize [B],cellCount, IBLT size [B], total keyCount,total encTime,total decTime,failureCount,successCount,failureProbability\n");
    }

    public void addResult(TestConfig config, ResultStats resultStats) {
        addDataPoint(config, resultStats);
        double failureProbability = (double)resultStats.getFailures() / (double)(resultStats.getFailures() + resultStats.getSuccesses());
        writer.printf(FORMAT, config.getTxCount(), config.getExtraTxCount(), config.getAbsentTxCount(), config.getHashFunctionCount(), config.getKeySize(),
                config.getValueSize(), config.getKeyHashSize(), config.getCellCount(), config.getIbltSize(), resultStats.getTotalResidualKeyCount(), resultStats.getTotalEncodingTime(), resultStats.getTotalDecodingTime(),
                resultStats.getFailures(), resultStats.getSuccesses(),
                failureProbability);
        writer.flush();
    }

    protected abstract void addDataPoint(TestConfig config, ResultStats resultStats);

    public void logResult(TestConfig config, ResultStats stats) {
        logger.info("CellCount: {}, successes: {}, failures: {}", config.getCellCount(),
                stats.getSuccesses(), stats.getFailures());
    }

    public void parseCsv(File csvFile) throws IOException {
        FileReader fileReader = new FileReader(csvFile);
        BufferedReader in = new BufferedReader(fileReader);
        String line = in.readLine();
        int version = -1;
        if (line == null) {
            return;
        }
        if (line.startsWith("version ")) {
            version = Integer.parseInt(line.substring("version ".length()).trim());
        }
        if (version < 1 || version > FILE_FORMAT_VERSION) {
            throw new RuntimeException("Invalid file format version " + version);
        }
        in.readLine(); // Skip headers

        while ((line = in.readLine()) != null) {
            if (line.trim().equals("")) {
                continue;
            }
            if (version == 1) {
                parseLineV1(line);
            } else if (version == 2) {
                parseLineV2(line);
            }
        }
        in.close();
    }

    private void parseLineV2(String line) {
        TestConfig config = new TestConfig();
        StringTokenizer tokenizer = new StringTokenizer(line, ",", false);

        int expectedTokens = 14;
        if (tokenizer.countTokens() != expectedTokens) {
            logger.error("Failed to parse line '{}'. Wrong number of tokens. Expected {} actual {}", line, expectedTokens, tokenizer.countTokens());
            throw new RuntimeException("Couldn't parse line '" + line + "'");
        }
        config.setTxCount(parseInt(tokenizer));
        config.setExtraTxCount(parseInt(tokenizer));
        config.setAbsentTxCount(parseInt(tokenizer));
        config.setHashFunctionCount(parseInt(tokenizer));
        config.setKeySize(parseInt(tokenizer));
        config.setValueSize(parseInt(tokenizer));
        config.setKeyHashSize(parseInt(tokenizer));
        config.setCellCount(parseInt(tokenizer));
        ResultStats result = new ResultStats();
        result.setTotalResidualKeyCount(parseInt(tokenizer));
        result.setTotalEncodingTime(parseInt(tokenizer));
        result.setTotalDecodingTime(parseInt(tokenizer));
        result.setFailures(parseInt(tokenizer));
        result.setSuccesses(parseInt(tokenizer));
        addResult(config, result);
    }

    private void parseLineV1(String line) {
        TestConfig config = new TestConfig();
        StringTokenizer tokenizer = new StringTokenizer(line, ",", false);

        int expectedTokens = 13;
        if (tokenizer.countTokens() != expectedTokens) {
            logger.error("Failed to parse line '{}'. Wrong number of tokens. Expected {} actual {}", line, expectedTokens, tokenizer.countTokens());
            throw new RuntimeException("Couldn't parse line '" + line + "'");
        }
        config.setExtraTxCount(parseInt(tokenizer));
        config.setTxCount(config.getExtraTxCount());
        config.setAbsentTxCount(parseInt(tokenizer));
        config.setHashFunctionCount(parseInt(tokenizer));
        config.setKeySize(parseInt(tokenizer));
        config.setValueSize(parseInt(tokenizer));
        config.setKeyHashSize(parseInt(tokenizer));
        config.setCellCount(parseInt(tokenizer));
        ResultStats result = new ResultStats();
        int averageKeyCount = parseInt(tokenizer);
        int averageEncodingTime = parseInt(tokenizer);
        int averageDecodingTime = parseInt(tokenizer);
        result.setFailures(parseInt(tokenizer));
        result.setSuccesses(parseInt(tokenizer));
        int sampleSize = result.getFailures() + result.getSuccesses();
        result.setTotalResidualKeyCount(averageKeyCount * sampleSize);
        result.setTotalEncodingTime(averageEncodingTime * sampleSize);
        result.setTotalDecodingTime(averageDecodingTime * sampleSize);
        addResult(config, result);
    }


    private int parseInt(StringTokenizer tokenizer) {
        return Integer.parseInt(tokenizer.nextToken());
    }

    protected void createLogarithmicProbabilityPrinter(Map<Comparable, List<DataPoint>> dataPoints, String title, String xAxisLabel, String yAxisLabel, File file) throws IOException {
        DefaultTableXYDataset dataSet = new DefaultTableXYDataset();
        for (Map.Entry<? extends Comparable, List<DataPoint>> listEntry : dataPoints.entrySet()) {
            XYSeries series = new XYSeries(listEntry.getKey(), true, false);
            for (DataPoint point : listEntry.getValue()) {
                series.add(point.category, point.yValue == 0 ? 0.000000001 : point.yValue);
            }
            dataSet.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataSet, PlotOrientation.VERTICAL,
                true, false, false);

        printChart(yAxisLabel, file, chart);
    }

    protected void createLogarithmicProbabilityPrinter(List<DataPoint> dataPoints, String title, String xAxisLabel, String yAxisLabel, File file) throws IOException {
        DefaultTableXYDataset dataSet = new DefaultTableXYDataset();
        XYSeries series = new XYSeries("1", true, false);
        for (DataPoint point : dataPoints) {
            series.add(point.category, point.yValue == 0 ? 0.000000001 : point.yValue);
        }
        dataSet.addSeries(series);
        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataSet, PlotOrientation.VERTICAL,
                false, false, false);

        printChart(yAxisLabel, file, chart);
    }

    private void printChart(String yAxisLabel, File file, JFreeChart chart) throws IOException {
        LogarithmicAxis yAxis = new LogarithmicAxis(yAxisLabel);

        XYPlot plot = chart.getXYPlot();
        plot.setRangeAxis(yAxis);

        plot.getDomainAxis().setVerticalTickLabels(true);
        plot.setRenderer(new XYLineAndShapeRenderer(true, true));

        BufferedImage image = chart.createBufferedImage(600,400);

        OutputStream out = new FileOutputStream(file);
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            logger.error("Failed to write image.", e);
        }
        out.close();
    }

    public static class DataPoint {
        int category;
        double yValue;

        public DataPoint(int category, double yValue) {
            this.category = category;
            this.yValue = yValue;
        }
    }
}
