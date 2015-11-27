package se.rosenbaum.bitcoiniblt.corpus;

import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;

class TransactionSetsTestConfig extends TestConfig
{
    TransactionSets transactionSets;

    public TransactionSetsTestConfig(TransactionSets sets, int hashFunctionCount, int keySize, int valueSize, int keyHashSize, int cellCount)
    {
        super(0, 0, 0, hashFunctionCount, keySize, valueSize, keyHashSize, cellCount);
        this.transactionSets = sets;
        setAbsentTxCount(sets.getReceiversTransactions().size());
        setExtraTxCount(sets.getSendersTransactions().size());
    }

    @Override
    public TransactionSets createTransactionSets()
    {
        return transactionSets;
    }

    @Override
    public boolean assertTransactionListCorrect() {
        return false;
    }
}
