package se.rosenbaum.bitcoiniblt.printer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.chart.BarChart;
import se.rosenbaum.bitcoiniblt.util.ResultStats;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CellCountFailureProbabilityPrinter extends BlockStatsPrinter {
    private static final Logger logger = LoggerFactory.getLogger(CellCountFailureProbabilityPrinter.class);
    private List<DataPoint> dataPoints = new ArrayList<DataPoint>();
    private static final String FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s,%.4f\n";



    public static class DataPoint {
        int category;
        double yValue;

        public DataPoint(int category, double yValue) {
            this.category = category;
            this.yValue = yValue;
        }
    }


    @Test
    public void testCreateImage() throws IOException {

        List<DataPoint> dataPoints = new ArrayList<DataPoint>();

        dataPoints.add(new DataPoint(100, 10000));
        dataPoints.add(new DataPoint(101, 9999));
        dataPoints.add(new DataPoint(200, 5000));
        dataPoints.add(new DataPoint(600, 4000));



        createImage(dataPoints);
    }

    public CellCountFailureProbabilityPrinter(File tempDirectory) throws IOException {
        super(tempDirectory);
        writer.printf("txcount,hashFunctionCount,keySize [B],valueSize [B]," +
                "keyHashSize [B],cellCount,failureCount,successCount,failureProbability\n");
    }

    public void addResult(TestConfig config, ResultStats resultStats) {

        double yValue = (double)resultStats.getFailures() / (double)(resultStats.getFailures() + resultStats.getSuccesses());
        DataPoint dataPoint = new DataPoint(config.getCellCount(), yValue);
        dataPoints.add(dataPoint);
        writer.printf(FORMAT, config.getTxCount(), config.getHashFunctionCount(), config.getKeySize(),
                config.getValueSize(), config.getKeyHashSize(), config.getCellCount(),
                resultStats.getFailures(), resultStats.getSuccesses(),
                yValue);
        writer.flush();
    }

    public void logResult(TestConfig config, ResultStats stats) {
        logger.info("CellCount: {}, successes: {}, failures: {}", config.getCellCount(),
                stats.getSuccesses(), stats.getFailures());
    }

    public void createImage(List<DataPoint> dataPoints) throws IOException {
        DefaultTableXYDataset dataSet = new DefaultTableXYDataset();
        XYSeries series = new XYSeries("APA", true, false);
        for (DataPoint point : dataPoints) {
            series.add(point.category, point.yValue == 0 ? 0.000000001 : point.yValue);
        }
        dataSet.addSeries(series);
        //dataSet.addValue(point.yValue == 0 ? 0.000000001 : point.yValue, "apa", Integer.valueOf(point.category));
        JFreeChart chart3 = ChartFactory.createXYLineChart("", "Number of cells", "Failure probability", dataSet, PlotOrientation.VERTICAL,
                false, false, false);

        LogarithmicAxis yAxis = new LogarithmicAxis("Failure probability");
        XYPlot plot = chart3.getXYPlot();
        plot.setRangeAxis(yAxis);

        plot.getDomainAxis().setVerticalTickLabels(true);
        BufferedImage image = chart3.createBufferedImage(600,400);

        OutputStream out = new FileOutputStream(getFile("_logarithmicline.png"));
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            logger.error("Failed to write image.", e);
        }
        out.close();
    }

    private void generateImage() throws IOException {
        createImage(dataPoints);
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
        for (DataPoint point : dataPoints) {
            dataSet.addValue(point.yValue, "apa", Integer.valueOf(point.category));
        }
        JFreeChart chart = ChartFactory.createBarChart("", "Number of cells", "Failure probability", dataSet, PlotOrientation.VERTICAL,
                false, false, false);

        BufferedImage image = chart.createBufferedImage(600,400);
        OutputStream out = new FileOutputStream(getFile("_bar.png"));
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            logger.error("Failed to write image.", e);
        }
        out.close();


        JFreeChart chart2 = ChartFactory.createLineChart("", "Number of cells", "Failure probability", dataSet, PlotOrientation.VERTICAL,
                false, false, false);
        image = chart2.createBufferedImage(600,400);
        out = new FileOutputStream(getFile("_line.png"));
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            logger.error("Failed to write image.", e);
        }
        out.close();

    }

    @Override
    public void finish() throws IOException {
        super.finish();
        generateImage();
    }

    @Override
    protected String filePrefix() {
        return "cellCount-failureProbability-Stats";
    }
}
