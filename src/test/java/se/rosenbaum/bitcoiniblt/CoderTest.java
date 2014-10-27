package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.easymock.EasyMockSupport;

import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.expect;

public abstract class CoderTest extends EasyMockSupport {
    private static final String ZERO = "0000000000000000000000000000000000000000000000000000000000000000";
    private int hashValue = 0;

    public Sha256Hash createHash(String shortVersion) {
        String hash64 = ZERO.substring(0, ZERO.length() - shortVersion.length()) + shortVersion;
        return new Sha256Hash(hash64);
    }

    public Transaction t(String... inputHashes) {
        Transaction t1 = createMock(Transaction.class);
        List<TransactionInput> inputs = new ArrayList<TransactionInput>();
        long i = 0;
        for (String inputHash : inputHashes) {
            TransactionInput input = createMock(TransactionInput.class);
            TransactionOutPoint outpoint = createMock(TransactionOutPoint.class);

            expect(input.getOutpoint()).andReturn(outpoint).anyTimes();
            expect(outpoint.getHash()).andReturn(createHash(inputHash)).anyTimes();
            expect(outpoint.getIndex()).andReturn(i++).anyTimes();
            inputs.add(input);
        }
        expect(t1.getInputs()).andReturn(inputs).anyTimes();
        expect(t1.getHash()).andReturn(createHash("" + hashValue++));
        return t1;
    }
}
