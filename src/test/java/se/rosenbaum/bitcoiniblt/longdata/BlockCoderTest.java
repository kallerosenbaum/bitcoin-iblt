package se.rosenbaum.bitcoiniblt.longdata;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.params.TestNet3Params;
import org.junit.Before;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.CanonicalOrderTransactionSorter;
import se.rosenbaum.bitcoiniblt.CoderTest;
import se.rosenbaum.bitcoiniblt.longdata.BlockCoder;
import se.rosenbaum.bitcoiniblt.longdata.LongDataTransactionCoder;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockCoderTest extends CoderTest {

    private BlockCoder sut;

    @Test
    public void testDependsOn1() {
        testDependsOn(true);
    }

    private void testDependsOn(boolean dependent) {
        Transaction t0 = t("1");
        Transaction t1 = t("2", "3", dependent ? createHash("0").toString() : "5");
        // t2 depends on t1
        replayAll();
        if (dependent) {
            assertTrue(sut.dependsOn(t1, t0));
        } else {
            assertFalse(sut.dependsOn(t1, t0));
        }
//        assertFalse(sut.dependsOn(t1, t2));
    }

    @Before
    public void setup() {
        LongDataTransactionCoder transactionCoder = new LongDataTransactionCoder(new TestNet3Params(), new byte[256/8]);
        sut = new BlockCoder(1, 1, transactionCoder, new CanonicalOrderTransactionSorter());
    }

    @Test
    public void testDependsOnFalse() {
        testDependsOn(false);
    }

    @Test
    public void testIteratorEmpty() {
        //Iterator<Transaction> result = sut.iterator(Collections.EMPTY_LIST);
        //assertFalse(result.hasNext());
    }

    @Test
    public void testIteratorSingle() {
        testIterator(new int[]{1}, new int[]{0});
    }

    @Test
    public void testIteratorTwoNonDependent() {
        testIterator(new int[]{20, 30}, new int[]{0, 1});
    }

    @Test
    public void testIteratorTwoDependentInOrder() {
        testIterator(new int[]{20, 0}, new int[]{0, 1});
    }

    @Test
    public void testIteratorTwoDependentNotInOrder() {
        testIterator(new int[]{1, 20}, new int[]{1, 0});
    }

    @Test
    public void testIteratorThreeDependentNotInOrder() {
        testIterator(new int[]{1, 2, 30}, new int[]{2, 1, 0});
    }

    @Test
    public void testIteratorThreeDependentInOrder() {
        testIterator(new int[]{30, 0, 1}, new int[]{0, 1, 2});
    }

    @Test
    public void testIteratorThreeDependentInMixedOrder() {
        testIterator(new int[]{1, 30, 1}, new int[]{1, 0, 2});
    }

    private void testIterator(int[] transactions, int[] expectedOrder) {
        List<Transaction> list = new ArrayList<Transaction>();
        for (int transaction : transactions) {
            list.add(t(transaction + ""));
        }
        replayAll();
        /*
        Iterator<Transaction> result = sut.iterator(list);
        assertNotNull(result);
        for (int transactionIndex : expectedOrder) {
            assertTrue(result.hasNext());
            assertEquals(createHash(transactionIndex + ""), result.next().getHash());
        }
        */
    }
}
