package se.rosenbaum.bitcoiniblt.util;

public class ResultStats {
    private int successes = 0;
    private int failures = 0;

    public void addSample(boolean success) {
        if (success) {
            successes++;
        } else {
            failures++;
        }
    }

    public int getSuccesses() {
        return successes;
    }

    public int getFailures() {
        return failures;
    }
}
