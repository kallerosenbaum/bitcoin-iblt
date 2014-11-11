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

public class CellCountFailureProbabilityPrinter extends FailureProbabilityPrinter {
    private static final Logger logger = LoggerFactory.getLogger(CellCountFailureProbabilityPrinter.class);
    private List<DataPoint> dataPoints = new ArrayList<DataPoint>();

    public CellCountFailureProbabilityPrinter(File tempDirectory) throws IOException {
        super(tempDirectory);

    }

    @Override
    protected void addDataPoint(TestConfig config, ResultStats resultStats) {
        double yValue = (double)resultStats.getFailures() / (double)(resultStats.getFailures() + resultStats.getSuccesses());
        DataPoint dataPoint = new DataPoint(config.getCellCount(), yValue);
        dataPoints.add(dataPoint);
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

    @Override
    protected String filePrefix() {
        return "cellCount-failureProbability-Stats";
    }


    @Override
    public void finish() throws IOException {
        super.finish();
        createImage(dataPoints);
    }
}
