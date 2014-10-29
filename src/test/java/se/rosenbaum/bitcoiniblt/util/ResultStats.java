package se.rosenbaum.bitcoiniblt.util;

/**
* User: kalle
* Date: 10/29/14 8:35 PM
*/
public class ResultStats {
    private int successes = 0;
    private int failures = 0;
    private TestConfig config;

    public ResultStats(TestConfig config) {
        this.setConfig(config);
    }

    public int getSuccesses() {
        return successes;
    }

    public void setSuccesses(int successes) {
        this.successes = successes;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public TestConfig getConfig() {
        return config;
    }

    public void setConfig(TestConfig config) {
        this.config = config;
    }
}
