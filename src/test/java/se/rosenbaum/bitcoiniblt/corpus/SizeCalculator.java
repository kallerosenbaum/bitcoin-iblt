package se.rosenbaum.bitcoiniblt.corpus;

import se.rosenbaum.bitcoiniblt.util.TestConfig;

public interface SizeCalculator {
    int getCellCount(IBLTBlockStream.IBLTBlockTransfer transfer, TestConfig config) throws Exception;

    int getUnknowns();

    int getUnknownBytes();
}
