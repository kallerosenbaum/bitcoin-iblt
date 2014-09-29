package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import se.rosenbaum.iblt.Cell;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.IntegerData;
import se.rosenbaum.iblt.data.LongData;
import se.rosenbaum.iblt.hash.LongDataHashFunction;
import se.rosenbaum.iblt.hash.LongDataSubtablesHashFunctions;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class TransactionCoderTest extends CoderTest {
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
        List<Transaction> transactions = block.getTransactions();

        TransactionCoder sut = new TransactionCoder(params, salt);

        for (Transaction transaction : transactions) {
            Map<LongData, LongData> map = sut.encodeTransaction(transaction);
            Transaction result = sut.decodeTransaction(map);
            assertEquals(transaction, result);
        }
    }
}
