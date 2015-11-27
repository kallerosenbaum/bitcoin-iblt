package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FullCorpusWithHintsTestConfigGenerator extends TestConfigGenerator
{
    IBLTBlockStream blockStream;
    List<IBLTBlockStream.IBLTBlockTransfer> transfers;
    IBLTBlockStream.IBLTBlockTransfer currentTransfer;
    File inputFileFromRustysBitcoinIblt;
    TransactionStore transactionStore;
    Map<Integer, Integer> blocksToProcess = null;

    FullCorpusWithHintsTestConfigGenerator(File inputFileFromRustysBitcoinIblt, int cellCount, TransactionStore transactionStore, boolean fileIncludesWeak, boolean processWeak) throws IOException
    {
        super(0, 0, 0, 3, 8, 64, 2, cellCount);
        this.inputFileFromRustysBitcoinIblt = inputFileFromRustysBitcoinIblt;
        this.transactionStore = transactionStore;
        blockStream = new IBLTBlockStream(this.inputFileFromRustysBitcoinIblt, this.transactionStore);
        if (!processWeak && fileIncludesWeak) {
            blocksToProcess = new HashMap<Integer, Integer>();
            // Skip all but the strong blocks. Keep in memory the number of blocks for each height
            List<IBLTBlockStream.IBLTBlockTransfer> tempTransfers = blockStream.getNextBlockTransfers();
            while (tempTransfers != null) {
                int height = tempTransfers.get(0).ibltBlock.getHeight();
                Integer occurrences = blocksToProcess.get(height);
                if (occurrences == null) {
                    blocksToProcess.put(height, 1);
                } else {
                    blocksToProcess.put(height, occurrences+1);
                }
                tempTransfers = blockStream.getNextBlockTransfers();
            }
        }
        blockStream = new IBLTBlockStream(this.inputFileFromRustysBitcoinIblt, this.transactionStore);
    }

    FullCorpusWithHintsTestConfigGenerator(File inputFileFromRustysBitcoinIblt, int cellCount, TransactionStore transactionStore) throws IOException
    {
        this(inputFileFromRustysBitcoinIblt, cellCount, transactionStore, false, false);
    }

    public TestConfig createNextTestConfig() throws Exception
    {
        if (!updateCurrentTransfer()) return null;

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

    private boolean updateCurrentTransfer() throws IOException {
        if (transfers == null || transfers.isEmpty())
        {
            transfers = blockStream.getNextBlockTransfers();
            if (transfers == null)
            {
                return false;
            }
            if (blocksToProcess != null) {
                int height = transfers.get(0).ibltBlock.getHeight();
                Integer count = blocksToProcess.get(height);
                blocksToProcess.put(height, count-1);
                while (count != 1) {
                    transfers = blockStream.getNextBlockTransfers();
                    if (transfers == null) {
                        return false;
                    }
                    height = transfers.get(0).ibltBlock.getHeight();
                    count = blocksToProcess.get(height);
                    blocksToProcess.put(height, count-1);
                }
            }
        }
        currentTransfer = transfers.remove(0);
        return true;
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
