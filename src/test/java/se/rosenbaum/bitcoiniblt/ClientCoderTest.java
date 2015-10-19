package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Message;
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
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.util.TransactionCollectorProcessor;
import se.rosenbaum.bitcoiniblt.util.TransactionProcessor;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;
import se.rosenbaum.iblt.data.ByteArrayData;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    protected String MAINNET_BLOCK = "0000000000000000152c3db4fe011716c4e5d41e44089e1da9d7d64917bb5011";

    // Test network block
    protected String TESTNET_BLOCK = "00000000e4a728571997c669c52425df5f529dd370fa9164c64fd60a49e245c4";

    protected File tempDirectory;
    private File blockDirectory;
    private File transactionDirectory;
    private File walletDirectory;
    private Sha256Hash[] blockHashes;
    private Random random;
    private Map<Sha256Hash, Block> blockCache = new HashMap<Sha256Hash, Block>();
    protected int blockCount = 1000;

    @Before
    public void setupWallet() throws ExecutionException, InterruptedException, BlockStoreException {
        salt = new byte[32];
        salt[14] = -45; // set something other than 0.
        String tmpDir = System.getProperty("iblt.output.dir", ".");
        logger.info("System property iblt.output.dir={}", System.getProperty("iblt.output.dir"));
        logger.info("tmpDir={}", new File(tmpDir).getAbsolutePath());

        tempDirectory = new File(new File(tmpDir), "data");
        blockDirectory = new File(tempDirectory, "blocks");
        transactionDirectory = new File(tempDirectory, "transactions");
        walletDirectory = new File(tempDirectory, "wallet");
        blockDirectory.mkdirs();
        transactionDirectory.mkdirs();
        walletDirectory.mkdirs();
    }

    protected void startWallet() {
        walletAppKit = new WalletAppKit(getParams(), walletDirectory, "iblt" + getParams().getClass().getSimpleName());
        walletAppKit.startAsync().awaitRunning();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (walletAppKit != null) {
            walletAppKit.stopAsync().awaitTerminated(1L, TimeUnit.MINUTES);
        }
    }

    public Transaction getTransaction(Sha256Hash transactionId) throws IOException {
        File txFile = new File(transactionDirectory, transactionId.toString());
        if (txFile.exists()) {
            byte[] fileContents = new byte[(int)txFile.length()];
            InputStream inputStream = new FileInputStream(txFile);
            if (fileContents.length != inputStream.read(fileContents)) {
                throw new RuntimeException("Couldn't read transaction " + transactionId + " from file " + txFile.getAbsolutePath());
            }
            return new Transaction(getParams(), fileContents);
        }
        return null;
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
                saveTransactions(block);
                return block;
            }

            if (walletAppKit == null) {
                startWallet();
            }
            block = walletAppKit.peerGroup().getDownloadPeer().getBlock(blockId).get();
            logger.debug("downladed!");
            saveBlockToDisk(block);
            saveTransactions(block);
            blockCache.put(blockId, block);
            return block;
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            return null;
        }
    }

    private void saveTransactions(Block block) {
        for (Transaction transaction : block.getTransactions()) {
            saveToDisk(transactionDirectory, transaction);
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
        saveToDisk(blockDirectory, block);
    }

    private void saveToDisk(File targetDirectory, Message message) {
        try {
            logger.debug("Saving " + message.getClass().getSimpleName() + " {} to disk ... ", message.getHash().toString());
            File blockFile = getFile(targetDirectory, message);
            if (blockFile.exists()) {
                return;
            }
            FileOutputStream out = new FileOutputStream(blockFile);
            message.bitcoinSerialize(out);
            out.close();
            logger.debug("done!");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private File getFile(File targetDirectory, Message message) {
        return new File(targetDirectory, message.getHash().toString());
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

        assertTrue(blockCount <= blockHashes.length);
        Random random = getRandom();
        int blockIndex = random.nextInt(blockCount);
        Block block = getBlock(blockHashes[blockIndex]);
        while (!coinbase && block.getTransactions().size() == 1) {
            blockIndex = random.nextInt(blockCount);
            block = getBlock(blockHashes[blockIndex]);
        }
        int txIndex = coinbase ? 0 : random.nextInt(block.getTransactions().size() - 1) + 1;
        return block.getTransactions().get(txIndex);
    }

    private Random getRandom() {
        if (random == null) {
            random = new Random();
        }
        return random;
    }

    protected List<Transaction> getRandomTransactions(int transactionCount, boolean includeCoinbase) {
        downloadBlocks(blockCount);
        Set<Transaction> result = new HashSet<Transaction>();

        if (transactionCount == 0) {
            return Collections.emptyList();
        }
        for (int i = includeCoinbase ? 1 : 0; i < transactionCount; i++) {
            Transaction randomTransaction = getRandomTransaction(false);
            while (result.contains(randomTransaction)) {
                randomTransaction = getRandomTransaction(false);
            }
            result.add(randomTransaction);
        }

        List<Transaction> txList = new ArrayList<Transaction>(result.size() + 1);
        if (includeCoinbase) {
            txList.add(getRandomTransaction(true)); // Add a coinbase
        }
        txList.addAll(result);
        return txList;
    }

    private void downloadBlocks(int count) {
        if (blockHashes != null && blockHashes.length == count) {
            // Already downloaded
            return;
        }
        blockHashes = new Sha256Hash[count];
        Block block = getBlock(new Sha256Hash(MAINNET_BLOCK));
        blockHashes[0] = block.getHash();
        for (int i = 1; i < count; i++) {
            block = getBlock(block.getPrevBlockHash());
            blockHashes[i] = block.getHash();
        }
    }

    protected void processBlocks(String startBlockHash, int count, BlockProcessor processor) {
        Block block = getBlock(new Sha256Hash(startBlockHash));
        int processedBlocks = 0;
        while (block != null && processedBlocks < count) {
            processor.process(block);
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


    protected TransactionSets createTransactionSets(int txCount, int extraCount, int absentCount, boolean randomSelection) {
        List<Transaction> collectedTransactions;
        if (randomSelection) {
            collectedTransactions = getRandomTransactions(txCount + absentCount, true);
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

    public class RandomTransactionsTestConfig extends TestConfig {
        private boolean isRandomTxSelection;

        public RandomTransactionsTestConfig(int txCount, int extraTxCount, int absentTxCount, int hashFunctionCount, int keySize, int valueSize, int keyHashSize, int cellCount, boolean randomTxSelection) {
            super(txCount, extraTxCount, absentTxCount, hashFunctionCount, keySize, valueSize, keyHashSize, cellCount);
            this.isRandomTxSelection = randomTxSelection;
        }

        public RandomTransactionsTestConfig(TestConfig other) {
            super(other);
        }

        @Override
        public TransactionSets createTransactionSets() {
            return ClientCoderTest.this.createTransactionSets(getTxCount(), getExtraTxCount(), getAbsentTxCount(), isRandomTxSelection);
        }

    }
}
