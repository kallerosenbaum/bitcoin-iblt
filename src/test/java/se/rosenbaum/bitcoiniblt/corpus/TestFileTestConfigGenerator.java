package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

class TestFileTestConfigGenerator extends TestConfigGenerator
{
    private final BufferedReader fileReader;
    private final File inputFile;
    TransactionStore transactionStore;

    public TestFileTestConfigGenerator(File file, int hashFunctionCount, int keySize, int valueSize, int keyHashSize, int cellCount, TransactionStore transactionStore)
            throws FileNotFoundException
    {
        super(0, 0, 0, hashFunctionCount, keySize, valueSize, keyHashSize, cellCount);
        this.inputFile = file;
        this.fileReader = new BufferedReader(new FileReader(file));
        this.transactionStore = transactionStore;
    }

    @Override
    public TestConfigGenerator cloneGenerator() throws Exception
    {
        return new TestFileTestConfigGenerator(inputFile, getHashFunctionCount(), getKeySize(), getValueSize(), getKeyHashSize(), getCellCount(), transactionStore);
    }

    public TestConfig createNextTestConfig()
    {
        TransactionSets nextTransactionSets = createNextTransactionSets();
        if (nextTransactionSets == null)
        {
            return null;
        }
        setExtraTxCount(nextTransactionSets.getSendersTransactions().size());
        setAbsentTxCount(nextTransactionSets.getReceiversTransactions().size());
        return new TransactionSetsTestConfig(nextTransactionSets, getHashFunctionCount(), getKeySize(), getValueSize(), getKeyHashSize(),
                getCellCount());
    }

    private TransactionSets createNextTransactionSets()
    {
        try
        {
            String line = fileReader.readLine();
            if (line == null)
            {
                fileReader.close();
                return null;
            }
            TransactionSets transactionSets = new TransactionSets();
            transactionSets.setSendersTransactions(new ArrayList<Transaction>());
            transactionSets.setReceiversTransactions(new ArrayList<Transaction>());
            line = line.substring("extra:".length());
            addTransactionsToList(line, transactionSets.getSendersTransactions());

            line = fileReader.readLine();
            line = line.substring("absent:".length());
            addTransactionsToList(line, transactionSets.getReceiversTransactions());
            return transactionSets;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void addTransactionsToList(String line, List<Transaction> transactions) throws Exception
    {
        StringTokenizer tokenizer = new StringTokenizer(line, ",");
        while (tokenizer.hasMoreTokens())
        {
            String hashString = tokenizer.nextToken();
            Transaction transaction = transactionStore.getTransaction(new Sha256Hash(hashString));
            if (transaction == null)
            {
                throw new RuntimeException("Couldn't find transaction " + hashString);
            }
            transactions.add(transaction);
        }
    }

    @Override
    public TransactionSets createTransactionSets()
    {
        return null;
    }

}
