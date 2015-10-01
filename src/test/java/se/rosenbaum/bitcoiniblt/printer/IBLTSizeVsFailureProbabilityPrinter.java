package se.rosenbaum.bitcoiniblt.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.util.ResultStats;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IBLTSizeVsFailureProbabilityPrinter extends FailureProbabilityPrinter {
    private static final Logger logger = LoggerFactory.getLogger(CellCountVSFailureProbabilityPrinter.class);
    private List<DataPoint> dataPoints = new ArrayList<DataPoint>();

    public IBLTSizeVsFailureProbabilityPrinter(File tempDirectory) throws IOException {
        super(tempDirectory);
    }

    @Override
    protected void addDataPoint(TestConfig config, ResultStats resultStats) {
        DataPoint dataPoint = new DataPoint(config.getIbltSize(), resultStats.getFailureProbability());
        dataPoints.add(dataPoint);
    }

    @Override
    protected String filePrefix() {
        return "IBLTSizeVsFailureProbability-";
    }


    @Override
    public void finish() throws IOException {
        super.finish();
        createLogarithmicProbabilityPrinter(dataPoints, "IBLT sizes around 5% failure probability", "IBLT size [B]", "Failure probability", getFile(".png"));
    }

}
