package se.rosenbaum.bitcoiniblt.corpus;

import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

public class RecordInputStreamTest {

    @Test
    public void testTest() {
        System.out.println("int 255 ->" + ((byte)255));
        System.out.println("int -1 ->" + ((byte)-1));
        System.out.println("int 127 ->" + ((byte)127));
        System.out.println("int 128 ->" + ((byte)128));
        System.out.println("byte -1 ->" + ((int) ((byte) -1)));

        System.out.println("0 & 0xff -> " + ((byte) -1 & 0xff));
    }

    byte[] b(int... bytes) {
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = (byte)(bytes[i] < 128 ? bytes[i] : bytes[i] - 256);
        }
        return result;
    }

    @Test
    public void testReadUnsignedInt() {
        RecordInputStream sut = new RecordInputStream(new ByteArrayInputStream(new byte[0]));
        assertEquals(4294967295L, sut.readUnsignedInt(b(255, 255, 255, 255), 0, 4));
        assertEquals(4294967294L, sut.readUnsignedInt(b(254, 255, 255, 255), 0, 4));
        assertEquals(1L, sut.readUnsignedInt(b(1, 0, 0, 0), 0, 4));
        assertEquals(16777216L, sut.readUnsignedInt(b(0, 0, 0, 1), 0, 4));
    }
}