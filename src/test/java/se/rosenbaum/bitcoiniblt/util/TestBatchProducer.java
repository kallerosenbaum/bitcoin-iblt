package se.rosenbaum.bitcoiniblt.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBatchProducer {
    private static final Logger logger = LoggerFactory.getLogger(TestBatchProducer.class);
    int cellCountIncrement = 60;

    public TestBatchProducer(int initialCellCountIncrement) {
        this.cellCountIncrement = initialCellCountIncrement;
    }

    public TestBatch nextTestBatch(TestBatch previousTestBatch, ResultStats result) {
        int failures = result.getFailures();
        int samples = previousTestBatch.getSampleCount();
        double probabilityThreshold = 0.0001;
        int maxSamples = (int)(10 / probabilityThreshold);   // 10 samples corresponds to the threshold
        if ((double)failures / (double)samples < probabilityThreshold) {
            // We're finished.
            return null;
        }
        int multiplier = 1;
        if (failures < 40) {
            multiplier = 40/failures;
        }
        cellCountIncrement = cellCountIncrement * (failures <= 20 ? 2 : 1);

        TestConfig nextConfig = new TestConfig(previousTestBatch.getTestConfig());
        nextConfig.setCellCount(previousTestBatch.getTestConfig().getCellCount() + cellCountIncrement);
        samples = Math.min(maxSamples, multiplier * samples);
        TestBatch nextBatch = new TestBatch(nextConfig, samples);
        logger.info("Next batch cellCount: {} sampleCount: {}", nextConfig.getCellCount(), samples);
        return nextBatch;
    }


}
