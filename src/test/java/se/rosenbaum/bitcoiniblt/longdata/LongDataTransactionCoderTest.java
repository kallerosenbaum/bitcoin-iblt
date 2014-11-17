package se.rosenbaum.bitcoiniblt.longdata;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.store.BlockStoreException;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.ClientCoderTest;
import se.rosenbaum.iblt.data.LongData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LongDataTransactionCoderTest extends ClientCoderTest {

    @Test
    public void testEncodeDecodeTransaction() throws BlockStoreException, ExecutionException, InterruptedException {
        List<Transaction> transactions = getBlock().getTransactions();
        assertFalse(transactions.isEmpty());

        LongDataTransactionCoder sut = new LongDataTransactionCoder(getParams(), salt);

        for (Transaction transaction : transactions) {
            Map<LongData, LongData> map = sut.encodeTransaction(transaction);
            Transaction result = sut.decodeTransaction(map);
            assertEquals(transaction, result);
        }
    }

    @Test
    public void testEncodeDecodeTransactions() throws BlockStoreException, ExecutionException, InterruptedException {
        List<Transaction> transactions = new ArrayList<Transaction>(getBlock().getTransactions());
        assertFalse(transactions.isEmpty());
        LongDataTransactionCoder sut = new LongDataTransactionCoder(getParams(), salt);
        Map<LongData, LongData> allData = new HashMap<LongData, LongData>();
        for (Transaction transaction : transactions) {
            Map<LongData, LongData> map = sut.encodeTransaction(transaction);
            allData.putAll(map);
        }

        List<Transaction> result = sut.decodeTransactions(allData);

        assertEquals(transactions.size(), result.size());
        for (Transaction transaction : result) {
            assertTrue(transactions.remove(transaction));
        }
        assertTrue(transactions.isEmpty());
    }
}
