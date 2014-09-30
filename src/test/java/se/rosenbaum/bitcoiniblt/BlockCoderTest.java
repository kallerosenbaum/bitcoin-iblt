package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import org.junit.Test;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockCoderTest extends CoderTest {

    @Test
    public void testDependsOn1() {
        Transaction t1 = t("1");
        Sha256Hash t1Hash = createHash("4");
        expect(t1.getHash()).andReturn(t1Hash).anyTimes();
        Transaction t2 = t("2", "3", t1Hash.toString());
        // t2 depends on t1
        replayAll();
        BlockCoder sut = new BlockCoder(10, 2);
       // assertTrue(sut.dependsOn(t2, t1));
        assertFalse(sut.dependsOn(t1, t2));
    }

    @Test
    public void testIterator() {

    }
}
