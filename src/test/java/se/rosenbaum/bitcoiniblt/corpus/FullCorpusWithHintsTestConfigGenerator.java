package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class FullCorpusWithHintsTestConfigGenerator extends TestConfigGenerator
{
    IBLTBlockStream blockStream;
    List<IBLTBlockStream.IBLTBlockTransfer> transfers;
    IBLTBlockStream.IBLTBlockTransfer currentTransfer;
    File inputFileFromRustysBitcoinIblt;
    TransactionStore transactionStore;

    FullCorpusWithHintsTestConfigGenerator(File inputFileFromRustysBitcoinIblt, int cellCount, TransactionStore transactionStore) throws IOException
    {
        super(0, 0, 0, 3, 8, 64, 4, cellCount);
        this.inputFileFromRustysBitcoinIblt = inputFileFromRustysBitcoinIblt;
        this.transactionStore = transactionStore;
        blockStream = new IBLTBlockStream(this.inputFileFromRustysBitcoinIblt, this.transactionStore);
    }

    public TestConfig createNextTestConfig() throws Exception
    {
        if (transfers == null || transfers.isEmpty())
        {
            transfers = blockStream.getNextBlockTransfers();
            if (transfers == null)
            {
                return null;
            }
        }
        currentTransfer = transfers.remove(0);

        List<Transaction> blockOnlyTransactions = new ArrayList<Transaction>();
        for (Sha256Hash blockOnly : currentTransfer.getBlockOnly())
        {
            Transaction transaction = transactionStore.getTransaction(blockOnly);
            if (transaction == null)
            {
                throw new RuntimeException("Couldn't find transaction " + blockOnly);
            }
            blockOnlyTransactions.add(transaction);
        }
        List<Transaction> mempoolOnlyTransactions = new ArrayList<Transaction>();
        for (Sha256Hash mempoolOnly : currentTransfer.getMempoolOnly())
        {
            Transaction transaction = transactionStore.getTransaction(mempoolOnly);
            if (transaction == null)
            {
                throw new RuntimeException("Couldn't find transaction " + mempoolOnly);
            }
            mempoolOnlyTransactions.add(transaction);
        }
        TransactionSets transactionSets = new TransactionSets();
        transactionSets.setReceiversTransactions(mempoolOnlyTransactions);
        transactionSets.setSendersTransactions(blockOnlyTransactions);
        TransactionSetsTestConfig testConfig = new TransactionSetsTestConfig(transactionSets, getHashFunctionCount(), getKeySize(), getValueSize(), getKeyHashSize(), getCellCount());
        return testConfig;
    }

    @Override
    public TestConfigGenerator cloneGenerator() throws Exception
    {
        return new FullCorpusWithHintsTestConfigGenerator(inputFileFromRustysBitcoinIblt, getCellCount(), transactionStore);
    }

    @Override
    public TransactionSets createTransactionSets()
    {
        return null;
    }
}
