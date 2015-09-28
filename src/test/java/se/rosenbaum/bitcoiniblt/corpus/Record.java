package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
class Record {
    long timestamp;
    Type type;
    int blockNumber;
    byte[] txid;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public String toString() {
        String result = dateFormat.format(new Date(timestamp*1000)) + " " + type + " " + blockNumber + " " + Utils.HEX.encode(txid);
        return result;
    }
}
