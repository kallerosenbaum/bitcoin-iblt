package se.rosenbaum.bitcoiniblt.corpus;

import com.google.common.io.LittleEndianDataInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class RecordInputStream {
    byte[] recordBytes = new byte[40];
    LittleEndianDataInputStream in;

    public RecordInputStream(InputStream in) {
        this.in = new LittleEndianDataInputStream(in);
    }

    /**
     Each record either reflects a new TX we accepted into the mempool from
     the network, OR the relative state of the mempool when a new block
     came in:

     [4-byte:timestamp] [1 byte:type] [3-byte:block number] [32-byte:txid]

     Where type is:
     1. INCOMING_TX: a new transaction added to the mempool, block number is 0.
     2. COINBASE: a coinbase transaction (ie. we didn't know this one)
     3. UNKNOWN: a transaction was in the new block, and we didn't know about it.
     4. KNOWN: a transaction was in the new block, and our mempool.
     5. MEMPOOL_ONLY: a transaction was in the mempool, but not the block.

     The ordering is:
     1. Zero or more INCOMING_TX.
     2. A COINBASE tx.
     3. Zero or more UNKNOWN and KNOWN txs, in any order.
     4. Zero or more MEMPOOL_ONLY txs.

     You can simply uncompress the corpora and load them directly into C
     arrays.  See example/simple-analysis.c.

     */
    public Record readRecord() throws IOException {
        int read = in.read(recordBytes);
        if (read == -1) {
            return null;
        }
        if (read != recordBytes.length) {
            throw new RuntimeException("Wrong number of bytes. Expected " + recordBytes.length + " got " + read);
        }
        Record record = new Record();

        record.timestamp = readUnsignedInt(recordBytes, 0, 4);
        record.type = Type.of((int) readUnsignedInt(recordBytes, 4, 1));
        record.blockNumber = (int)readUnsignedInt(recordBytes, 5, 3);
        record.txid = Arrays.copyOfRange(recordBytes, 8, 40);
        return record;
    }

    long readUnsignedInt(byte[] bytes, int offset, int length) {
        long s = 0L;
        for (int i = offset + length - 1; i >= offset; i--) {
            s = (s << 8);
            s = s | (bytes[i] & 0xff);
        }
        return s;
    }

    public void close() throws IOException {
        in.close();
    }
}
