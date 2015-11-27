package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class FullCorpusWithHintsDynamicCellCountTestConfigGenerator extends FullCorpusWithHintsTestConfigGenerator {
    private int totalBytesSent = 0;
    private int rawBlocksSent = 0;

    private SizeCalculator sizeCalculator;

    public FullCorpusWithHintsDynamicCellCountTestConfigGenerator(SizeCalculator sizeCalculator, File inputFileFromRustysBitcoinIblt, TransactionStore transactionStore, boolean fileIncludesWeak, boolean processWeak) throws IOException {
        super(inputFileFromRustysBitcoinIblt, 0, transactionStore, fileIncludesWeak, processWeak);
        this.sizeCalculator = sizeCalculator;
    }

    public FullCorpusWithHintsDynamicCellCountTestConfigGenerator(SizeCalculator sizeCalculator, File inputFileFromRustysBitcoinIblt, TransactionStore transactionStore) throws IOException {
        this(sizeCalculator, inputFileFromRustysBitcoinIblt, transactionStore, false, false);
    }

    @Override
    public TestConfigGenerator cloneGenerator() throws Exception {
        return new FullCorpusWithHintsDynamicCellCountTestConfigGenerator(sizeCalculator, inputFileFromRustysBitcoinIblt, transactionStore);
    }

    @Override
    public TestConfig createNextTestConfig() throws Exception {
        TestConfig nextTestConfig = super.createNextTestConfig();
        if (nextTestConfig == null) {
            return null;
        }
        int cellCount = sizeCalculator.getCellCount(currentTransfer, nextTestConfig);
        int remainder = cellCount % getHashFunctionCount();
        nextTestConfig.setCellCount(remainder == 0 ? cellCount : cellCount - remainder + getHashFunctionCount());

        int ibltSize = nextTestConfig.getIbltSize();

        int messageSize = ibltSize + currentTransfer.getOverhead();
        int rawBlockSize = currentTransfer.getOverhead();
        Set<Sha256Hash> blockTransactions = currentTransfer.ibltBlock.ibltData.transactions;
        for (Sha256Hash blockTransaction : blockTransactions) {
            Transaction transaction = transactionStore.getTransaction(blockTransaction);
            rawBlockSize += transaction.getOptimalEncodingMessageSize();
        }

        if (messageSize >= rawBlockSize) {
            rawBlocksSent++;
            totalBytesSent += rawBlockSize;
            return createNextTestConfig();
        }

        totalBytesSent += messageSize;
        return nextTestConfig;
    }

    public int getTotalBytesSent() {
        return totalBytesSent;
    }

    public int getRawBlocks() {
        return rawBlocksSent;
    }
}
