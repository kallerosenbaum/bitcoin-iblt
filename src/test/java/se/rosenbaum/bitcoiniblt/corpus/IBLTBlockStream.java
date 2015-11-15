package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class IBLTBlockStream {
    private final TransactionStore transactionStore;
    BufferedReader inputReader;

    public static class MempoolData {
        String nodeName;
        Set<Sha256Hash> nodeMempool;

        public MempoolData(String nodeName, Set<Sha256Hash> nodeMempool) {
            this.nodeMempool = nodeMempool;
            this.nodeName = nodeName;
        }

        public String getNodeName() {
            return nodeName;
        }
    }

    public static class IBLTBlock {
        int overhead;
        int height;
        MempoolData ibltData;

        public IBLTBlock(MempoolData ibltData, int height, int overhead) {
            this.ibltData = ibltData;
            this.height = height;
            this.overhead = overhead;
        }

        public int getHeight() {
            return height;
        }
    }

    public class IBLTBlockTransfer {
        IBLTBlock ibltBlock;
        MempoolData receiverTxGuessData; // This is the guesses after filtering through fee-hint and added/removed sets.

        private IBLTBlockTransfer(IBLTBlock ibltBlock, MempoolData receiverTxGuessData) {
            this.ibltBlock = ibltBlock;
            this.receiverTxGuessData = receiverTxGuessData;
        }

        public Set<Sha256Hash> getMempoolOnly() {
            Set<Sha256Hash> mempoolOnly = new HashSet<Sha256Hash>();
            Set<Sha256Hash> ibltTxs = ibltBlock.ibltData.nodeMempool;
            for (Sha256Hash guessTx : receiverTxGuessData.nodeMempool) {
                if (!ibltTxs.contains(guessTx)) {
                    mempoolOnly.add(guessTx);
                }
            }
            return mempoolOnly;
        }

        public Set<Sha256Hash> getBlockOnly() throws Exception {
            Set<Sha256Hash> blockOnly = new HashSet<Sha256Hash>();
            for (Sha256Hash ibltTx : ibltBlock.ibltData.nodeMempool) {
                if (!receiverTxGuessData.nodeMempool.contains(ibltTx)) {
                    blockOnly.add(ibltTx);
                }
            }
            return blockOnly;
        }

        public int getOverhead() {
            return ibltBlock.overhead;
        }
    }

    public IBLTBlockStream(File inputFile, TransactionStore transactionStore) throws IOException {
        this.transactionStore = transactionStore;
        inputReader = new BufferedReader(new FileReader(inputFile));
    }

    List<IBLTBlockTransfer> getNextBlockTransfers() throws IOException {
        IBLTBlock ibltBlock = readIBLTBlock();
        if (ibltBlock == null) { // End of file
            return null;
        }
        List<IBLTBlockTransfer> transfers = new ArrayList<IBLTBlockTransfer>();
        for (int i = 0; i < 3; i++) {
            transfers.add(readIBLTBlockTransfer(ibltBlock));
        }
        return transfers;
    }

    private IBLTBlock readIBLTBlock() throws IOException {
        String line = inputReader.readLine();
        if (line == null) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(line, ",");
        if (!"block".equals(tokenizer.nextToken())) {
            throw new RuntimeException("Invalid block line: " + line);
        }
        int height = Integer.parseInt(tokenizer.nextToken());
        int overhead = Integer.parseInt(tokenizer.nextToken());

        MempoolData ibltData = readMempoolData(); // iblt data

        MempoolData senderMempool = readMempoolData();

        ibltData.nodeName = senderMempool.getNodeName(); // We only use senderMempool to the the node name
        return new IBLTBlock(ibltData, height, overhead);
    }


    private IBLTBlockTransfer readIBLTBlockTransfer(IBLTBlock ibltBlock) throws IOException {
        MempoolData mempoolData = readMempoolData();
        return new IBLTBlockTransfer(ibltBlock, mempoolData);
    }

    private MempoolData readMempoolData() throws IOException {
        String line;
        StringTokenizer tokenizer;
        line = inputReader.readLine();
        tokenizer = new StringTokenizer(line, ",");
        if (!"mempool".equals(tokenizer.nextToken())) {
            throw new RuntimeException("Invalid mempool line: " + line);
        }
        String name = tokenizer.nextToken();
        name = name.substring(name.lastIndexOf('/') + 1);
        Set<Sha256Hash> mempoolTransactions = getTransactionSet(tokenizer);

        return new MempoolData(name, mempoolTransactions);
    }
    private Set<Sha256Hash> getTransactionSet(StringTokenizer tokenizer) {
        Set<Sha256Hash> transactions = new HashSet<Sha256Hash>();
        while (tokenizer.hasMoreTokens()) {
            transactions.add(new Sha256Hash(tokenizer.nextToken()));
        }
        return transactions;
    }

}
