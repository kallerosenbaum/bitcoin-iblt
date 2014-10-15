package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.*;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import org.junit.After;
import org.junit.Before;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This class can be used as a super class for test cases
 * where a bitcoin client is needed, for example when
 * test cases need to pull blocks from the block
 * chain.
 */
public abstract class ClientCoderTest extends CoderTest {
    protected byte[] salt;

    private WalletAppKit walletAppKit = null;

    // Production network block.
    public static final String MAINNET_BLOCK = "0000000000000000152c3db4fe011716c4e5d41e44089e1da9d7d64917bb5011";

    // Test network block
    public static final String TESTNET_BLOCK = "00000000e4a728571997c669c52425df5f529dd370fa9164c64fd60a49e245c4";

    private static String BLOCK = null;

    private static Map<Sha256Hash, Block> blockCache = new HashMap<Sha256Hash, Block>();
    private File tempDirectory;
    private static final String BLOCK_DIRECTORY = "blocks";

    @Before
    public void setupWallet() throws ExecutionException, InterruptedException, BlockStoreException {
        salt = new byte[32];
        salt[14] = -45; // set something other than 0.
        String tmpDir = System.getProperty("java.io.tmpdir");
        tempDirectory = new File(tmpDir);
    }

    private void startWallet() {
        walletAppKit = new WalletAppKit(getParams(), tempDirectory, "iblt" + getParams().getClass().getSimpleName());
        walletAppKit.startAndWait();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (walletAppKit != null) {
            walletAppKit.stopAndWait();
        }
    }

    public Block getBlock(Sha256Hash blockId) {
        try {
            System.out.print("Getting block " + blockId + "... ");
            if (blockCache.containsKey(blockId)) {
                System.out.println("found in cache!");
                return blockCache.get(blockId);
            }

            if (walletAppKit == null) {
                startWallet();
            }
            Block block = walletAppKit.peerGroup().getDownloadPeer().getBlock(blockId).get();
            System.out.println("downladed!");
            blockCache.put(blockId, block);
            return block;
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            return null;
        }
    }

    private Block readBlock(Sha256Hash blockHash) {
        System.out.print("Reading block " + block.getHash().toString() + "... ");
        File blockFile = new File(new File(tempDirectory, BLOCK_DIRECTORY), blockHash.toString());
        if (!blockFile.exists()) {
            return null;
        }

        try {
            FileInputStream fileInput = new FileInputStream(blockFile);
            ByteArrayInputStream in = new ByteArrayInputStream(fileInput);

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        Block block = new Block()
    }

    private void saveBlock(Block block) {
        try {
            System.out.print("Saving block " + block.getHash().toString() + "... ");
            File blockFile = new File(new File(tempDirectory, BLOCK_DIRECTORY), block.getHashAsString());
            FileOutputStream out = new FileOutputStream(blockFile);
            block.bitcoinSerialize(out);
            out.close();
            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public Block getBlock() {
        Sha256Hash blockHash = new Sha256Hash(TESTNET_BLOCK);
        return getBlock(blockHash);
    }

    public NetworkParameters getParams() {
        return new TestNet3Params();
    }

    protected void processTransactions(String startBlockHash, int count, TransactionProcessor processor) {
        Sha256Hash blockHash = new Sha256Hash(startBlockHash);
        Block currentBlock = getBlock(blockHash);
        int fetchCount = count;
        int processedCount = 0;
        outerLoop:
        try {
            while (processedCount < fetchCount) {
                List<Transaction> otherTransactions = currentBlock.getTransactions();
                for (int i = 0; i < otherTransactions.size(); i++) {
                    processor.process(otherTransactions.get(i));
                    if (processedCount++ == fetchCount) {
                        break outerLoop;
                    }
                }
                currentBlock = getBlock(currentBlock.getPrevBlockHash());
            }
        } catch (StopProcessingException e) {

        }
    }

    protected List<Transaction> getMyTransactions(List<Transaction> blockTransactions, int extraCount, int absentCount) {
        List<Transaction> myTransactions = new ArrayList<Transaction>(blockTransactions.size());
        // Mess up the ordering
        for (int i = blockTransactions.size() - 1; i >= 0; i--) {
           myTransactions.add(blockTransactions.get(i));
        }

        // Remove some transactions from my "mempool"
        assertTrue(extraCount <= myTransactions.size());
        for (int i = 0; i < extraCount; i++) {
            myTransactions.remove(myTransactions.size() - 2 - (i*(myTransactions.size()-2)/extraCount));
        }

        // Add some transactions to my "mempool" that is not in the IBLT
        TransactionAdder txAdder = new TransactionAdder(myTransactions);
        processTransactions(getBlock().getPrevBlockHash().toString(), absentCount, txAdder);

        return myTransactions;
    }

    private class TransactionAdder implements TransactionProcessor {
        private List<Transaction> transactions;

        private TransactionAdder(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        @Override
        public void process(Transaction transaction) {
            transactions.add(transaction);
        }
    }

}
