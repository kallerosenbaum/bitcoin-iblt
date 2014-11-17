package se.rosenbaum.bitcoiniblt.bytearraydata;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteArrayKeyData {
    private byte[] bytes;

    public ByteArrayKeyData(byte[] bytes) {
        this.bytes = bytes;
    }
    /*
    public ByteArrayKeyData(int sizeInBytes) {
        super(sizeInBytes);
    }

*/

    public void setIndex(char index) {
        byte[] keyCounterBytes = ByteBuffer.allocate(2).putChar(index).array();
        bytes[bytes.length - 2] = keyCounterBytes[0];
        bytes[bytes.length - 1] = keyCounterBytes[1];
    }

    public char getIndex() {
        return ByteBuffer.wrap(bytes, bytes.length - 2, 2).getChar();
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }
/*
    @Override
    public KeyData<ByteArrayData> copyKeyData() {
        return new ByteArrayKeyData(this);
    }*/
}
