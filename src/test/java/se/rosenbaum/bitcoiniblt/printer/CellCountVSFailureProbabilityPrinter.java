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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CellCountVSFailureProbabilityPrinter extends FailureProbabilityPrinter {
    private static final Logger logger = LoggerFactory.getLogger(CellCountVSFailureProbabilityPrinter.class);
    private Map<Integer, List<DataPoint>> dataPointLists = new HashMap<Integer, List<DataPoint>>();

    public CellCountVSFailureProbabilityPrinter(File tempDirectory) throws IOException {
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

    @Override
    protected String filePrefix() {
        return "onePercentLine-Stats";
    }


    @Override
    public void finish() throws IOException {
        super.finish();
        for (Map.Entry<Integer, List<DataPoint>> entry : dataPointLists.entrySet()) {
            createLogarithmicProbabilityPrinter(entry.getValue(), entry.getKey() + " differences", "Number of cells", "Failure probability", getFile("_" + entry.getKey() + "_diffs.png"));
        }
    }

}
