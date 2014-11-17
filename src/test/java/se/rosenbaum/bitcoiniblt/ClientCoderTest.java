package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStoreException;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.util.TransactionCollectorProcessor;
import se.rosenbaum.bitcoiniblt.util.TransactionProcessor;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private Sha256Hash[] hashes;
    private Random random;
    private Map<Sha256Hash, Block> blockCache = new HashMap<Sha256Hash, Block>();

    @Before
    public void setupWallet() throws ExecutionException, InterruptedException, BlockStoreException {
        salt = new byte[32];
        salt[14] = -45; // set something other than 0.
        String tmpDir = System.getProperty("iblt.output.dir", ".");
        logger.info("System property iblt.output.dir={}", System.getProperty("iblt.output.dir"));
        logger.info("tmpDir={}", new File(tmpDir).getAbsolutePath());

        tempDirectory = new File(new File(tmpDir), "data");
        blockDirectory = new File(tempDirectory, "blocks");
        walletDirectory = new File(tempDirectory, "wallet");
        blockDirectory.mkdirs();
        walletDirectory.mkdirs();
    }

    private void startWallet() {
        walletAppKit = new WalletAppKit(getParams(), walletDirectory, "iblt" + getParams().getClass().getSimpleName());
        walletAppKit.startAsync().awaitRunning();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (walletAppKit != null) {
            walletAppKit.stopAsync().awaitTerminated(1L, TimeUnit.MINUTES);
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
        assertTrue(1000 <= hashes.length);
        if (random == null) {
            random = new Random();
        }
        int blockIndex = random.nextInt(1000);
        Block block = getBlock(hashes[blockIndex]);
        while (!coinbase && block.getTransactions().size() == 1) {
            blockIndex = random.nextInt(1000);
            block = getBlock(hashes[blockIndex]);
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
        hashes = new Sha256Hash[count];
        Block block = getBlock(new Sha256Hash(MAINNET_BLOCK));
        hashes[0] = block.getHash();
        for (int i = 1; i < count; i++) {
            block = getBlock(block.getPrevBlockHash());
            hashes[i] = block.getHash();
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


    protected TransactionSets createTransactionSets(int txCount, int extraCount, int absentCount, boolean randomSelection) {
        List<Transaction> collectedTransactions;
        if (randomSelection) {
            collectedTransactions = getRandomTransactions(txCount + absentCount);
        } else {
            collectedTransactions = getSameOldTransactions(txCount + absentCount);
        }

        assertTrue(txCount + absentCount == collectedTransactions.size());
        assertTrue(extraCount + absentCount <= collectedTransactions.size());

        TransactionSets sets = new TransactionSets();
        sets.setReceiversTransactions(new ArrayList<Transaction>(collectedTransactions));
        sets.setSendersTransactions(new ArrayList<Transaction>(collectedTransactions));
        List<Transaction> send = sets.getSendersTransactions();
        List<Transaction> rec = sets.getReceiversTransactions();

        send.removeAll(send.subList(rec.size() - absentCount, rec.size()));
        rec.removeAll(rec.subList(0, extraCount));

        return sets;
    }

    private List<Transaction> getSameOldTransactions(int txCount) {
        TransactionCollectorProcessor transactionCollector = new TransactionCollectorProcessor(txCount);

        processTransactions(MAINNET_BLOCK, Integer.MAX_VALUE, transactionCollector);
        return transactionCollector.getTransactions();
    }
}
