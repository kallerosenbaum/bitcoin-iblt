package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import org.easymock.EasyMockSupport;

import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.expect;

public class CoderTest extends EasyMockSupport {
    private static final String ZERO = "0000000000000000000000000000000000000000000000000000000000000000";

    public Sha256Hash createHash(String shortVersion) {
        String hash64 = ZERO.substring(0, ZERO.length() - shortVersion.length()) + shortVersion;
        return new Sha256Hash(hash64);
    }

    public Transaction t(String... hashes) {
        Transaction t1 = createMock(Transaction.class);
        List<TransactionInput> inputs = new ArrayList<TransactionInput>();
        for (String hash : hashes) {
            TransactionInput input = createMock(TransactionInput.class);
            expect(input.getHash()).andReturn(createHash(hash)).anyTimes();
            inputs.add(input);
        }
        expect(t1.getInputs()).andReturn(inputs).anyTimes();
        return t1;
    }
}
