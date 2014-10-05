package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.TestNet3Params;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.util.concurrent.ExecutionException;

public abstract class ClientCoderTest extends CoderTest {
    static protected byte[] salt;
    static protected NetworkParameters params;
    static protected Block block;
    private static WalletAppKit walletAppKit;

    @BeforeClass
    public static void setup() throws ExecutionException, InterruptedException {
        params = new TestNet3Params();
        walletAppKit = new WalletAppKit(params, new File(System.getProperty("java.io.tmpdir")), "iblt");
        walletAppKit.startAndWait();
        Sha256Hash blockHash = new Sha256Hash("00000000e4a728571997c669c52425df5f529dd370fa9164c64fd60a49e245c4");
        block = walletAppKit.peerGroup().getDownloadPeer().getBlock(blockHash).get();

        salt = new byte[256/8];
        salt[14] = -45;
    }

    @AfterClass
    public static void tearDown() throws ExecutionException, InterruptedException {
        walletAppKit.stopAndWait();
    }
}
