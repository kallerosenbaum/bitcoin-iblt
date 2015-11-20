package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;

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

    public static class TransactionSet {
        String nodeName;
        Set<Sha256Hash> transactions;

        public TransactionSet(String nodeName, Set<Sha256Hash> transactions) {
            this.transactions = transactions;
            this.nodeName = nodeName;
        }

        public String getNodeName() {
            return nodeName;
        }
    }

    public static class IBLTBlock {
        int overhead;
        int height;
        TransactionSet ibltData;
        TransactionSet senderMempool;

        public IBLTBlock(TransactionSet ibltData, TransactionSet sendarMempool, int height, int overhead) {
            this.ibltData = ibltData;
            this.height = height;
            this.overhead = overhead;
            this.senderMempool = sendarMempool;
        }

        public int getHeight() {
            return height;
        }
    }

    public class IBLTBlockTransfer {
        IBLTBlock ibltBlock;
        TransactionSet receiverTxGuessData; // This is the guesses after filtering through fee-hint and added/removed sets.

        private IBLTBlockTransfer(IBLTBlock ibltBlock, TransactionSet receiverTxGuessData) {
            this.ibltBlock = ibltBlock;
            this.receiverTxGuessData = receiverTxGuessData;
        }

        public Set<Sha256Hash> getMempoolOnly() {
            Set<Sha256Hash> mempoolOnly = new HashSet<Sha256Hash>();
            Set<Sha256Hash> ibltTxs = ibltBlock.ibltData.transactions;
            for (Sha256Hash guessTx : receiverTxGuessData.transactions) {
                if (!ibltTxs.contains(guessTx)) {
                    mempoolOnly.add(guessTx);
                }
            }
            return mempoolOnly;
        }

        public Set<Sha256Hash> getBlockOnly() throws Exception {
            Set<Sha256Hash> blockOnly = new HashSet<Sha256Hash>();
            for (Sha256Hash ibltTx : ibltBlock.ibltData.transactions) {
                if (!receiverTxGuessData.transactions.contains(ibltTx)) {
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

        Set<Sha256Hash> blockTransactions = getTransactionSet(tokenizer);

        TransactionSet senderMempool = readMempoolDataLine();
        TransactionSet ibltData = new TransactionSet(senderMempool.getNodeName(), blockTransactions);

        return new IBLTBlock(ibltData, senderMempool, height, overhead);
    }


    private IBLTBlockTransfer readIBLTBlockTransfer(IBLTBlock ibltBlock) throws IOException {
        TransactionSet transactionSet = readMempoolDataLine();
        return new IBLTBlockTransfer(ibltBlock, transactionSet);
    }

    private TransactionSet readMempoolDataLine() throws IOException {
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

        return new TransactionSet(name, mempoolTransactions);
    }
    private Set<Sha256Hash> getTransactionSet(StringTokenizer tokenizer) {
        Set<Sha256Hash> transactions = new HashSet<Sha256Hash>();
        while (tokenizer.hasMoreTokens()) {
            transactions.add(new Sha256Hash(tokenizer.nextToken()));
        }
        return transactions;
    }

}
