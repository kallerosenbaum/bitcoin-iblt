package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.*;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

    @Before
    public void setupWallet() throws ExecutionException, InterruptedException, BlockStoreException {
        salt = new byte[32];
        salt[14] = -45; // set something other than 0.
    }

    private void startWallet() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        walletAppKit = new WalletAppKit(getParams(), new File(tmpDir), "iblt" + getParams().getClass().getSimpleName());
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
    }
}
