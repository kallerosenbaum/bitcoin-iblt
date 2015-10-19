package se.rosenbaum.bitcoiniblt.util;

public class AggregateResultStats {
    private int successes = 0;
    private int failures = 0;
    private int totalResidualKeyCount = 0;
    private long totalEncodingTime = 0;
    private long totalDecodingTime = 0;

    public void addSample(BlockStatsResult result) {
        if (result.isSuccess()) {
            successes++;
        } else {
            failures++;
        }
        totalResidualKeyCount += result.getResidualKeysCount();
        totalEncodingTime += result.getEncodingTime();
        totalDecodingTime += result.getDecodingTime();
    }

    public int getSuccesses() {
        return successes;
    }

    public int getFailures() {
        return failures;
    }

    public double getFailureProbability() {
        if (failures + successes == 0) {
            return Double.MAX_VALUE;
        }
        return (double)getFailures() / (double)(successes + failures);
    }

    public long getTotalEncodingTime() {
        return totalEncodingTime;
    }

    public long getTotalDecodingTime() {
        return totalDecodingTime;
    }

    public int getTotalResidualKeyCount() {
        return totalResidualKeyCount;
    }

    public void setSuccesses(int successes) {
        this.successes = successes;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public void setTotalResidualKeyCount(int totalResidualKeyCount) {
        this.totalResidualKeyCount = totalResidualKeyCount;
    }

    public void setTotalEncodingTime(long totalEncodingTime) {
        this.totalEncodingTime = totalEncodingTime;
    }

    public void setTotalDecodingTime(long totalDecodingTime) {
        this.totalDecodingTime = totalDecodingTime;
    }
}
