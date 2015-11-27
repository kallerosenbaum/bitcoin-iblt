package se.rosenbaum.bitcoiniblt.corpus;

import se.rosenbaum.bitcoiniblt.util.TestConfig;

public class ModifiedRustysSizeCalculator extends RustysSizeCalculator {

    public ModifiedRustysSizeCalculator(int minimumTx, TransactionStore transactionStore) {
        super(minimumTx, transactionStore, 1);
    }

    @Override
    public int getCellCount(IBLTBlockStream.IBLTBlockTransfer transfer, TestConfig config) throws Exception {
        int slices = getSliceCount(transfer, config);
        slices += sliceCountForBytes(INITIAL_TX_SIZE, config.getValueSize()) * minimumTx;
        return getCellCountForSlices(slices);
    }

    protected int getSliceCount(IBLTBlockStream.IBLTBlockTransfer currentTransfer, TestConfig config) throws Exception {
        int slices = getUnknownSlices(currentTransfer, config);

        // Stats from full corpus block 352304-353024:
        // Mempool-only total = 4642
        // Block-only total = 7574
        slices = slices + (slices * 4642)/7574;

        return slices;
    }

    protected int getCellCountForSlices(int slices) {
        for (int[] slicesToCells : fivePercentFailureProb) {
            if (slicesToCells[0] > slices) {
                return slicesToCells[1];
            }
        }
        throw new RuntimeException("Can't find cellCount for " + slices + " slices");
    }
}
