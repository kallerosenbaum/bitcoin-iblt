package se.rosenbaum.bitcoiniblt.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.util.AggregateResultStats;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.File;
import java.io.IOException;
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
    protected void addDataPoint(TestConfig config, AggregateResultStats aggregateResultStats) {
        int diffs = config.getAbsentTxCount() + config.getExtraTxCount();
        DataPoint dataPoint = new DataPoint(config.getCellCount(), aggregateResultStats.getFailureProbability());
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
