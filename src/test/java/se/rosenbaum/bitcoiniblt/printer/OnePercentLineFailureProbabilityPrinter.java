package se.rosenbaum.bitcoiniblt.printer;

import junit.framework.TestResult;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberTickUnit;
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
import java.io.*;
import java.util.*;

public class OnePercentLineFailureProbabilityPrinter extends FailureProbabilityPrinter {
    private static final Logger logger = LoggerFactory.getLogger(OnePercentLineFailureProbabilityPrinter.class);
    private Map<Integer, List<DataPoint>> dataPointLists = new HashMap<Integer, List<DataPoint>>();

    public OnePercentLineFailureProbabilityPrinter(File tempDirectory) throws IOException {
        super(tempDirectory);
    }

    @Override
    protected void addDataPoint(TestConfig config, ResultStats resultStats) {
        int diffs = config.getAbsentTxCount() + config.getExtraTxCount();
        DataPoint dataPoint = new DataPoint(config.getCellCount(), resultStats.getFailureProbability());
        List<DataPoint> list = dataPointLists.get(diffs);
        if (list == null) {
            list = new ArrayList<DataPoint>();
            dataPointLists.put(diffs, list);
        }
        list.add(dataPoint);
    }

    private void createImage(List<DataPoint> dataPoints, Integer diffCount) throws IOException {
        DefaultTableXYDataset dataSet = new DefaultTableXYDataset();
        XYSeries series = new XYSeries("" + diffCount, true, false);
        for (DataPoint point : dataPoints) {
            series.add(point.category, point.yValue == 0 ? 0.000000001 : point.yValue);
        }
        dataSet.addSeries(series);
        //dataSet.addValue(point.yValue == 0 ? 0.000000001 : point.yValue, "apa", Integer.valueOf(point.category));
        JFreeChart chart3 = ChartFactory.createXYLineChart(diffCount + " differences", "Number of cells", "Failure probability", dataSet, PlotOrientation.VERTICAL,
                false, false, false);


        LogarithmicAxis yAxis = new LogarithmicAxis("Failure probability");

        XYPlot plot = chart3.getXYPlot();
        plot.setRangeAxis(yAxis);

        plot.getDomainAxis().setVerticalTickLabels(true);
        plot.setRenderer(new XYLineAndShapeRenderer(true, true));

        BufferedImage image = chart3.createBufferedImage(600,400);

        OutputStream out = new FileOutputStream(getFile("_" + diffCount + "_diffs.png"));
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            logger.error("Failed to write image.", e);
        }
        out.close();
    }

    @Override
    protected String filePrefix() {
        return "onePercentLine-Stats";
    }


    @Override
    public void finish() throws IOException {
        super.finish();
        for (Map.Entry<Integer, List<DataPoint>> entry : dataPointLists.entrySet()) {
            createImage(entry.getValue(), entry.getKey());
        }
    }

}
