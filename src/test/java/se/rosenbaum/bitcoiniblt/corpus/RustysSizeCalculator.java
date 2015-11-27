package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.util.TestConfig;

import java.util.Set;
/*
This is rusty's results:
INITIAL_TXS             EXTRA_FACTOR            Size            Number Correct (of 2112) probability
1	1	13843452	1885	0.1074810606
2	1	15564618	1949	0.0771780303
3	1	16886340	1972	0.0662878788
1	1.1	14652786	1912	0.0946969697
2	1.1	16516038	1983	0.0610795455
3	1.1	17979912	1999	0.0535037879

I should match it with this implementation
        */

public class RustysSizeCalculator implements SizeCalculator {
    private TransactionStore transactionStore;
    private final double extraFactor;
    protected final int minimumTx;
    protected static final int INITIAL_TX_SIZE = 300;
    int unknowns = 0;
    int unknownBytes;

    public RustysSizeCalculator(int minimumTx, TransactionStore transactionStore, double extraFactor) {
        this.minimumTx = minimumTx;
        this.transactionStore = transactionStore;
        this.extraFactor = extraFactor;
    }

    @Override
    public int getCellCount(IBLTBlockStream.IBLTBlockTransfer transfer, TestConfig config) throws Exception {
        int slices = sliceCountForBytes(INITIAL_TX_SIZE, config.getValueSize()) * minimumTx;
        slices += getUnknownSlices(transfer, config);

        int cellCount = getCellCountForSlices(slices);
        return (int)(cellCount * extraFactor);
    }

    @Override
    public int getUnknowns() {
        return unknowns;
    }

    @Override
    public int getUnknownBytes() {
        return unknownBytes;
    }

    protected int getUnknownSlices(IBLTBlockStream.IBLTBlockTransfer currentTransfer, TestConfig config) throws Exception {
        int slices = 0;
        unknowns = 0;
        unknownBytes = 0;

        Set<Sha256Hash> blockTransactions = currentTransfer.ibltBlock.ibltData.transactions;
        Set<Sha256Hash> senderMempoolTransactions = currentTransfer.ibltBlock.senderMempool.transactions;
        for (Sha256Hash blockTransaction : blockTransactions) {
            if (!senderMempoolTransactions.contains(blockTransaction)) {
                Transaction transaction = transactionStore.getTransaction(blockTransaction);
                int bytes = transaction.getOptimalEncodingMessageSize();
                slices += sliceCountForBytes(bytes, config.getValueSize());
                unknowns++;
                unknownBytes += bytes;
            }
        }
        return slices;
    }

    protected int getCellCountForSlices(int slices) {
        if (slices == 0) {
            return 0;
        }
        for (int i = 1; i < fivePercentFailureProb.length; i++) {
            if (fivePercentFailureProb[i][0] > slices) {
                return fivePercentFailureProb[i-1][1];
            }
        }

        throw new RuntimeException("Can't find cellCount for " + slices + " slices");
    }

    protected int sliceCountForBytes(int bytes, int valueSize) {
        return (valueSize + bytes + 1) / valueSize;
    }

    // This maps number of diff slices to number of cells needed.
    protected static final int[][] fivePercentFailureProb = new int[][] {
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
}
