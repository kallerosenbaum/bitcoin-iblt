package se.rosenbaum.bitcoiniblt.corpus;

import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

public interface TestListener {
    void testPerformed(TestConfig config, BlockStatsResult result);
}
