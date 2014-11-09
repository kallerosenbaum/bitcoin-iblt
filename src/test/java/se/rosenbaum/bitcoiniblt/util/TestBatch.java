package se.rosenbaum.bitcoiniblt.util;

public class TestBatch {
    private TestConfig testConfig;
    private int samples;

    public TestBatch(TestConfig testConfig, int samples) {
        this.testConfig = testConfig;
        this.samples = samples;
    }

    public TestConfig getTestConfig() {
        return testConfig;
    }

    public int getSampleCount() {
        return samples;
    }
}
