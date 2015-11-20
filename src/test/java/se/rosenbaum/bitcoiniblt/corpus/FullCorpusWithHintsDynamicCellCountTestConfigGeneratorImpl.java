package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class FullCorpusWithHintsDynamicCellCountTestConfigGeneratorImpl extends FullCorpusWithHintsTestConfigGenerator {
    private int totalIBLTSize = 0;
    int unknowns = 0;

    // This maps number of diff slices to number of cells needed.
    private static final int[][] fivePercentFailureProb = new int[][] {
        { 1,5 },
        { 2,9 },
        { 3,12 }, /* 4x */
        { 4,15 },
        { 5,17 },
        { 6,20 },
        { 7,22 },
        { 8,24 },
        { 9,27 }, /* 3x */
        { 10,29 },
        { 11,31 },
        { 12,32 },
        { 13,34 },
        { 14,36 },
        { 15,38 },
        { 16,40 },
        { 17,41 },
        { 18,43 },
        { 19,45 },
        { 20,46 },
        { 21,48 },
        { 22,49 },
        { 23,51 },
        { 24,53 },
        { 25,54 },
        { 26,56 },
        { 27,57 },
        { 28,58 },
        { 29,60 },
        { 30,61 }, /* 2x */
        { 40,75 },
        { 50,88 },
        { 60,100 },
        { 70,112 },
        { 80,124 },
        { 90,137 },
        { 100,149 }, /* 1.5x */
        { 200,273 },
        { 300,398 },
        { 400,524 },
        { 500,649 },
        { 600,774 },
        { 700,898 },
        { 800,1023 },
        { 900,1147 },
        { 1000,1271 },
        { 2000,2510 },
        { 3000,3745 },
        { 4000,4978 },
        { 5000,6209 },
        { 6000,7440 },
        { 7000,8669 },
        { 8000,9898 },
        { 9000,11127 },
        { 10000,12355 }
    };

    private final int minimumTx;
    private static final int INITIAL_TX_SIZE = 300;
    int unknownBytes;

    FullCorpusWithHintsDynamicCellCountTestConfigGeneratorImpl(File inputFileFromRustysBitcoinIblt, int minimumTx, TransactionStore transactionStore) throws IOException {
        super(inputFileFromRustysBitcoinIblt, 0, transactionStore);
        this.minimumTx = minimumTx;
    }

    @Override
    public TestConfigGenerator cloneGenerator() throws Exception {
        return new FullCorpusWithHintsDynamicCellCountTestConfigGeneratorImpl(inputFileFromRustysBitcoinIblt, minimumTx, transactionStore);
    }

    @Override
    public TestConfig createNextTestConfig() throws Exception {
        TestConfig nextTestConfig = super.createNextTestConfig();
        if (nextTestConfig == null) {
            return null;
        }
        int cellCount = calculateCellCount();
        int remainder = cellCount % getHashFunctionCount();
        nextTestConfig.setCellCount(remainder == 0 ? cellCount : cellCount - remainder + getHashFunctionCount());
        totalIBLTSize += nextTestConfig.getIbltSize();
        return nextTestConfig;
    }

    private int calculateCellCount() throws Exception {
        int slices = 0;
        unknowns = 0;
        unknownBytes = 0;

        Set<Sha256Hash> blockTransactions = currentTransfer.ibltBlock.ibltData.transactions;
        Set<Sha256Hash> senderMempoolTransactions = currentTransfer.ibltBlock.senderMempool.transactions;
        for (Sha256Hash blockTransaction : blockTransactions) {
            if (!senderMempoolTransactions.contains(blockTransaction)) {
                Transaction transaction = transactionStore.getTransaction(blockTransaction);
                int bytes = transaction.getOptimalEncodingMessageSize();
                slices += sliceCountForBytes(bytes);
                unknowns++;
                unknownBytes += bytes;
            }
        }

        // Stats from full corpus block 352304-353024:
        //Mempool-only total = 4642
        //Block-only total = 7574
        slices = slices + (slices * 4642)/7574;

        int minimumCells = sliceCountForBytes(INITIAL_TX_SIZE) * minimumTx;
        if (slices < minimumCells) {
            slices = minimumCells;
        }

        return getCellCountForSlices(slices);
    }

    private int getCellCountForSlices(int slices) {
        for (int[] slicesToCells : fivePercentFailureProb) {
            if (slicesToCells[0] > slices) {
                return slicesToCells[1];
            }
        }
        throw new RuntimeException("Can't find cellCount for " + slices + " slices");
    }

    private int sliceCountForBytes(int bytes) {
        return (getValueSize() + bytes + 1) / getValueSize();
    }

    public int getTotalIBLTSize() {
        return totalIBLTSize;
    }
}
