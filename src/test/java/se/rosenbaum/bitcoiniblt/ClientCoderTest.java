package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.*;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.TestNet3Params;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.easymock.EasyMock.expect;

public abstract class ClientCoderTest extends CoderTest {
    protected byte[] salt;
    NetworkParameters params;
    Block block;

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        params = new TestNet3Params();
        WalletAppKit walletAppKit = new WalletAppKit(params, new File(System.getProperty("java.io.tmpdir")), "iblt");
        walletAppKit.startAndWait();
        Sha256Hash blockHash = new Sha256Hash("00000000e4a728571997c669c52425df5f529dd370fa9164c64fd60a49e245c4");
        block = walletAppKit.peerGroup().getDownloadPeer().getBlock(blockHash).get();

        salt = new byte[256/8];
        salt[14] = -45;
    }

}
