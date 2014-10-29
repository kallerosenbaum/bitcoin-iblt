package se.rosenbaum.bitcoiniblt;

import com.google.common.io.PatternFilenameFilter;
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStoreException;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This class can be used as a super class for test cases
 * where a bitcoin client is needed, for example when
 * test cases need to pull blocks from the block
 * chain.
 */
public abstract class ClientCoderTest extends CoderTest {
    private static Logger logger = LoggerFactory.getLogger(ClientCoderTest.class);

    protected byte[] salt;

    private WalletAppKit walletAppKit = null;

    // Production network block.
    public static final String MAINNET_BLOCK = "0000000000000000152c3db4fe011716c4e5d41e44089e1da9d7d64917bb5011";

    // Test network block
    public static final String TESTNET_BLOCK = "00000000e4a728571997c669c52425df5f529dd370fa9164c64fd60a49e245c4";

    protected File tempDirectory;
    private File blockDirectory;
    private File walletDirectory;
    private String[] files;
    private Random random;
    private Map<Sha256Hash, Block> blockCache = new HashMap<Sha256Hash, Block>();

    @Before
    public void setupWallet() throws ExecutionException, InterruptedException, BlockStoreException {
        salt = new byte[32];
        salt[14] = -45; // set something other than 0.
        String tmpDir = System.getProperty("iblt.output.dir", ".");
        tempDirectory = new File(new File(tmpDir), "data");
        blockDirectory = new File(tempDirectory, "blocks");
        walletDirectory = new File(tempDirectory, "wallet");
        blockDirectory.mkdirs();
        walletDirectory.mkdirs();
    }

    private void startWallet() {
        walletAppKit = new WalletAppKit(getParams(), walletDirectory, "iblt" + getParams().getClass().getSimpleName());
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
            logger.debug("Getting block {}", blockId);
            Block block = blockCache.get(blockId);
            if (block != null) {
                logger.debug("found in cache!");
                return block;
            }
            block = readBlockFromDisk(blockId);
            if (block != null) {
                logger.debug("found on file system!");
                blockCache.put(blockId, block);
                return block;
            }

            if (walletAppKit == null) {
                startWallet();
            }
            block = walletAppKit.peerGroup().getDownloadPeer().getBlock(blockId).get();
            logger.debug("downladed!");
            saveBlockToDisk(block);
            blockCache.put(blockId, block);
            return block;
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            return null;
        }
    }

    private Block readBlockFromDisk(Sha256Hash blockHash) {
        logger.debug("Reading block {} from disk ... ", blockHash.toString());
        File blockFile = new File(blockDirectory, blockHash.toString());
        if (!blockFile.exists()) {
            logger.debug(" not found!");
            return null;
        }
        try {

            FileInputStream fileInput = new FileInputStream(blockFile);
            byte[] blockData = new byte[(int) blockFile.length()];
            fileInput.read(blockData);
            assertEquals(0, fileInput.available());
            fileInput.close();
            logger.debug(" found!");
            return new Block(getParams(), blockData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    private void saveBlockToDisk(Block block) {
        try {
            logger.debug("Saving block {} to disk ... ", block.getHash().toString());
            File blockFile = new File(blockDirectory, block.getHashAsString());
            FileOutputStream out = new FileOutputStream(blockFile);
            block.bitcoinSerialize(out);
            out.close();
            logger.debug("done!");
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
        return TestNet3Params.get();
    }

    // I know, I know. It's not random since different blocks contains
    // different number of tx. Transactions in Blocks with few transactions
    // will have a higher frequency, but who gives?
    private Transaction getRandomTransaction(boolean coinbase) {
        if (files == null) {
            files = blockDirectory.list(new PatternFilenameFilter("0{10}[0-9a-f]{54}"));
        }

        assertTrue(1000 <= files.length);
        if (random == null) {
            random = new Random();
        }
        int blockIndex = random.nextInt(1000);
        Block block = getBlock(new Sha256Hash(files[blockIndex]));
        while (!coinbase && block.getTransactions().size() == 1) {
            blockIndex = random.nextInt(1000);
            block = getBlock(new Sha256Hash(files[blockIndex]));
        }
        int txIndex = coinbase ? 0 : random.nextInt(block.getTransactions().size() - 1) + 1;
        return block.getTransactions().get(txIndex);
    }

    protected List<Transaction> getRandomTransactions(int transactionCount) {
        downloadBlocks(1000);
        List<Transaction> result = new ArrayList<Transaction>(transactionCount);

        if (transactionCount == 0) {
            return result;
        }
        // First get a random coinbase tx
        result.add(getRandomTransaction(true));

        for (int i = 1; i < transactionCount; i++) {
            Transaction randomTransaction = getRandomTransaction(false);
            while (result.contains(randomTransaction)) {
                randomTransaction = getRandomTransaction(false);
            }
            result.add(randomTransaction);
        }
        return result;
    }

    private void downloadBlocks(int count) {
        Block block = getBlock(new Sha256Hash(MAINNET_BLOCK));
        for (int i = 1; i < count; i++) {
            block = getBlock(block.getPrevBlockHash());
        }
    }

    protected void processTransactions(String startBlockHash, int count, TransactionProcessor processor) {
        Sha256Hash blockHash = new Sha256Hash(startBlockHash);
        Block currentBlock = getBlock(blockHash);
        int fetchCount = count;
        int processedCount = 0;
        try {
            outerLoop:
            while (processedCount < fetchCount) {
                List<Transaction> otherTransactions = currentBlock.getTransactions();
                for (int i = 0; i < otherTransactions.size(); i++) {
                    processor.process(otherTransactions.get(i));
                    if (++processedCount == fetchCount) {
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
