package se.rosenbaum.bitcoiniblt.corpus;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CorpusDataTestManual {

    private CorpusData corpusData;

    @Before
    public void setup() {
        corpusData = new CorpusData(new File("/home/kalle/hack/git/rusty/bitcoin-corpus"));
    }

    @Test
    public void testAU() throws IOException {
        RecordInputStream recordInputStream = corpusData.getRecordInputStream(CorpusData.Node.AU);
        Record record = recordInputStream.readRecord();
        while (record != null) {
            System.out.println(record.toString());
            record = recordInputStream.readRecord();
        }
    }

    @Test
    public void testAUListBlocks() throws IOException {
        RecordInputStream recordInputStream = corpusData.getRecordInputStream(CorpusData.Node.AU);
        Record record = recordInputStream.readRecord();
        while (record != null) {
            if (Type.COINBASE == record.type) {
                System.out.println(record.toString());
            }
            record = recordInputStream.readRecord();
        }
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

    /*
    1. Collect all extras (not coinbases) for all blocks and nodes from corpus. Calculate the average extras, E, over the remaining.
    2. Calculate the average tx rate, R, over the corpus. Sum the number of transactions in all blocks and divide it with the data collection period in seconds.
    3. Now calculate the "extras per tx rate", E/R.
    4. Absents, A, is calculated from E and the ratio extras/absent
    5. Assume that E/R is constant and that the extras/absent ratio holds for all tx rates.
    */
    @Test
    public void testExtrasPerTxRate() throws IOException {
        corpusData.calculateStatistics();

        System.out.println("Average transaction rate  : " + corpusData.txRate);
        System.out.println("Average extras per block  : " + corpusData.averageExtrasPerBlock);
        System.out.println("Average extras per tx rate: " + corpusData.extrasPerTxRate);
    }


}