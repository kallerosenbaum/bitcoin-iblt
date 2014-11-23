package se.rosenbaum.bitcoiniblt.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.util.ResultStats;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DiffCountVSFailureProbabilityPrinter extends FailureProbabilityPrinter {
    private static final Logger logger = LoggerFactory.getLogger(DiffCountVSFailureProbabilityPrinter.class);
    private Map<Comparable, List<DataPoint>> dataPointLists = new TreeMap<Comparable, List<DataPoint>>();

    public DiffCountVSFailureProbabilityPrinter(File tempDirectory) throws IOException {
        super(tempDirectory);
    }

    @Override
    protected void addDataPoint(TestConfig config, ResultStats resultStats) {
        int diffs = config.getAbsentTxCount() + config.getExtraTxCount();
        DataPoint dataPoint = new DataPoint(diffs, resultStats.getFailureProbability());
        String key = "k=" + config.getHashFunctionCount();
        List<DataPoint> list = dataPointLists.get(key);
        if (list == null) {
            list = new ArrayList<DataPoint>();
            dataPointLists.put(key, list);
        }
        list.add(dataPoint);
    }

    @Override
    protected String filePrefix() {
        return "DiffCountVSFailureProbability";
    }


    @Override
    public void finish() throws IOException {
        super.finish();
        createLogarithmicProbabilityPrinter(dataPointLists, "Failure prob vs diff count", "Number of diffs", "Failure probability", getFile(".png"));
    }

}
