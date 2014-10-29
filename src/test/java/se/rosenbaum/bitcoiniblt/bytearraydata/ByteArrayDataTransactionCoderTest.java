package se.rosenbaum.bitcoiniblt.bytearraydata;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.junit.Test;
import se.rosenbaum.iblt.data.ByteArrayData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: kalle
 * Date: 10/28/14 9:34 PM
 */
public class ByteArrayDataTransactionCoderTest {
//    @Test
    public void testDecodeTransactions() throws Exception {
        ByteArrayDataTransactionCoder sut = new ByteArrayDataTransactionCoder(MainNetParams.get(), new byte[32], 8, 1);
        Map<ByteArrayData, ByteArrayData> map = new HashMap<ByteArrayData, ByteArrayData>();
        map.put(data(new byte[] {-108, -10, -97, -100, -13, 120, 0, 0}), data(new byte[] {1}));
        map.put(data(new byte[] {-104, -123, -101, -33, -29, 98, 0, 0}), data(new byte[] {2}));
        sut.decodeTransactions(map);
    }

    ByteArrayData data(byte[] val) {
        return new ByteArrayData(val);
    }
}
