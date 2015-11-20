package se.rosenbaum.bitcoiniblt.corpus;

import se.rosenbaum.bitcoiniblt.util.TestConfig;

public abstract class TestConfigGenerator extends TestConfig
{

    public TestConfigGenerator(int txCount, int extraTxCount, int absentTxCount, int hashFunctionCount, int keySize, int valueSize,
							   int keyHashSize, int cellCount)
    {
        super(txCount, extraTxCount, absentTxCount, hashFunctionCount, keySize, valueSize, keyHashSize, cellCount);
    }

    public abstract TestConfig createNextTestConfig() throws Exception;

    public abstract TestConfigGenerator cloneGenerator() throws Exception;
}
