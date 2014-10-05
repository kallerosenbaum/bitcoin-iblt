package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Transaction;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CanonicalOrderTransactionSorterTest extends CoderTest {
    CanonicalOrderTransactionSorter sut;

    @Before
    public void setup() {
        sut = new CanonicalOrderTransactionSorter();
    }

    @Test
    public void testSameTransaction() {
        Transaction t = t("8", "1");
        replayAll();
        assertEquals(0, sut.compare(t, t));
    }

    @Test
    public void testCompare1() {
        testLt(t("0"), t("1"));    // first < second
    }

    @Test
    public void testCompare2() {
        testLt(t("8", "7", "6"), t("7"));
    }

    @Test
    public void testCompare3() {
        testLt(t("6", "7"), t("7"));
    }

    @Test
    public void testCompare4() {
        testLt(t("5", "7"), t("6"));
    }

    @Test
    public void testCompare5() {
        testLt(t("6", "8", "4"), t("5", "7"));
    }

    @Test
    public void testFullHashes() {
        testLt(t("e86265acd5e1179bce0daa291d326c084c64d2528f2de00edbe3e38b99bb2e51"),
                t("f86265acd5e1179bce0daa291d326c084c64d2528f2de00edbe3e38b99bb2e51"));
    }

    @Test
    public void testFullHashes2() {
        testLt(t("e86265acd5e1179bce0daa291d326c084c64d2528f2de00edbe3e38b99bb2e51"),
                t("e86265acd5e1179bce0daa291d326c084c64d2528f2de00edbe3e38b99bb2e52"));
    }

    @Test
    public void testFullHashesEqual() {
        Transaction t1 = t("e86265acd5e1179bce0daa291d326c084c64d2528f2de00edbe3e38b99bb2e51");
        Transaction t2 = t("e86265acd5e1179bce0daa291d326c084c64d2528f2de00edbe3e38b99bb2e51");
        replayAll();
        assertEquals(0, sut.compare(t1, t2));
        assertEquals(0, sut.compare(t2, t1));
    }

    private void testLt(Transaction t1, Transaction t2) {
        replayAll();
        lt(t1, t2);
    }

    private void lt(Transaction t1, Transaction t2) {
        assertTrue(sut.compare(t1, t2) < 0);
        assertTrue(sut.compare(t2, t1) > 0);
    }

}
