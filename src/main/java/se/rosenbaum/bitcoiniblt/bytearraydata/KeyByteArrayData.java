package se.rosenbaum.bitcoiniblt.bytearraydata;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.KeyData;
import se.rosenbaum.iblt.data.ByteArrayData;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class KeyByteArrayData extends ByteArrayData implements KeyData<ByteArrayData> {

    public KeyByteArrayData(Transaction transaction, int sizeInBytes, byte[] salt) {
        super(sizeInBytes);
        byte[] transactionId = transaction.getHash().getBytes();
        byte[] key = Arrays.copyOf(transactionId, transactionId.length + salt.length);
        System.arraycopy(salt, 0, key, transactionId.length, salt.length);
        key = Sha256Hash.create(key).getBytes();

        // 64 first bits (the last two bytes will be overwritten by counter
        System.arraycopy(key, 0, byteArray, 0, sizeInBytes - 2);
    }

    public KeyByteArrayData(ByteArrayData of) {
        super(of.getValue());
    }

    @Override
    public void setIndex(char index) {
        byte[] keyCounterBytes = ByteBuffer.allocate(2).putChar(index).array();
        byteArray[byteArray.length - 2] = keyCounterBytes[0];
        byteArray[byteArray.length - 1] = keyCounterBytes[1];
    }

    @Override
    public ByteArrayData getHashPart() {
        return new ByteArrayData(Arrays.copyOf(byteArray, byteArray.length - 2));
    }

    @Override
    public char getIndexPart() {
        return ByteBuffer.wrap(byteArray, byteArray.length - 2, 2).getChar();
    }

}
