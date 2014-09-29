package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import org.junit.Test;
import se.rosenbaum.iblt.Cell;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.IntegerData;
import se.rosenbaum.iblt.data.LongData;
import se.rosenbaum.iblt.hash.LongDataHashFunction;
import se.rosenbaum.iblt.hash.LongDataSubtablesHashFunctions;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class CoderTest {
    LongDataHashFunction cellHashFunction = new LongDataHashFunction();

    private LongData data(long value) {
        return new LongData(value);
    }

    private Cell<LongData, LongData> createCell() {
        Cell<LongData, LongData> cell = new Cell<LongData, LongData>(data(0), data(0), new IntegerData(0),
                cellHashFunction);
        return cell;
    }

    private Cell<LongData, LongData>[] createCells(int cellCount) {
        Cell<LongData, LongData>[] cells = new Cell[cellCount];
        for (int i = 0; i < cellCount; i++) {
            cells[i] = createCell();
        }
        return cells;
    }

    private IBLT<LongData, LongData> createIBLT() {
        return new IBLT<LongData, LongData>(createCells(10), new LongDataSubtablesHashFunctions(10, 2));
    }

    @Test
    public void testEncodeDecodeTransaction() throws BlockStoreException, ExecutionException, InterruptedException {
        TestNet3Params params = new TestNet3Params();
        WalletAppKit walletAppKit = new WalletAppKit(params, new File("/tmp"), "iblt");
        walletAppKit.startAndWait();
        Sha256Hash blockHash = new Sha256Hash("00000000e4a728571997c669c52425df5f529dd370fa9164c64fd60a49e245c4");
        ListenableFuture<Block> blockFuture = walletAppKit.peerGroup().getDownloadPeer().getBlock(blockHash);
        Block block = blockFuture.get();
        List<Transaction> transactions = block.getTransactions();

        byte[] salt = new byte[256/8];
        salt[14] = -45;

        Coder sut = new Coder(params, salt);

        for (Transaction transaction : transactions) {
            Map<LongData, LongData> map = sut.encodeTransaction(transaction);
            Transaction result = sut.decodeTransaction(map);
            assertEquals(transaction, result);
        }
    }
}
