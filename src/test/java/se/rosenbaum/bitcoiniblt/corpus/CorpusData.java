package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CorpusData {
    private File corpusDirectory;

    int extraCount = 0;
    int txCount = 0;
    int blockCount = 0;
    long timespan = 0;
    double txRate = 0;
    double averageExtrasPerBlock = 0;
    double extrasPerTxRate = 0;
    int highestBlock;
    Set<Integer> orphans;
    Set<Integer> blocks;
    Set<Sha256Hash> transactions = new HashSet<Sha256Hash>();
    public static final String HIGHEST_BLOCK_HASH = "000000000000000011defeab02396f6982807ec911a9294a0c8411d0c5f2f5a3";
    public CorpusData(File corpusDirectory) {
        this.corpusDirectory = corpusDirectory;
    }

    public enum Node {
        SG("sg"), AU("au"), SF("sf"), SF_RN("sf-rn");
        String fileName;

        Node(String fileName) {
            this.fileName = fileName;
        }
    }


    public RecordInputStream getRecordInputStream(Node node) throws IOException {
        FileInputStream inputStream = new FileInputStream(new File(corpusDirectory, node.fileName));
//        XZCompressorInputStream decompressor = new XZCompressorInputStream(inputStream);
        RecordInputStream recordInputStream = new RecordInputStream(inputStream);
        return recordInputStream;
    }

    public void calculateStatistics() throws IOException {
        Record record;
        Record lastRecord = null;

        Set<Integer> foundBlocks = new HashSet<Integer>();
        Set<Integer> deadBlocks = new HashSet<Integer>();
        RecordInputStream recordInputStream = getRecordInputStream(Node.AU);
        record = recordInputStream.readRecord();
        while (record != null) {
            if (timespan == 0) {
                timespan = record.timestamp;
            }
            if (!deadBlocks.contains(record.blockNumber)) {
                if (record.type == Type.UNKNOWN) {
                    extraCount++;
                    txCount++;
                    transactions.add(new Sha256Hash(record.txid));
                } else if (record.type == Type.KNOWN) {
                    txCount++;
                    transactions.add(new Sha256Hash(record.txid));
                } else if (record.type == Type.COINBASE) {
                    if (!foundBlocks.contains(record.blockNumber)) {
                        foundBlocks.add(record.blockNumber);
                        blockCount++;
                        this.highestBlock = record.blockNumber;
                    } else {
                        deadBlocks.add(record.blockNumber);
                    }
                }
            }
            lastRecord = record;
            record = recordInputStream.readRecord();
        }
        this.orphans = deadBlocks;
        this.blocks = foundBlocks;
        timespan = lastRecord.timestamp - timespan;
        txRate = (double)txCount / timespan;

        for (CorpusData.Node node : new Node[] {Node.SF, Node.SF_RN, Node.SG}) {
            foundBlocks = new HashSet<Integer>();
            deadBlocks = new HashSet<Integer>();
            recordInputStream = getRecordInputStream(node);
            record = recordInputStream.readRecord();
            while (record != null) {
                if (!deadBlocks.contains(record.blockNumber)) {
                    if (record.type == Type.UNKNOWN) {
                        extraCount++;
                    } else if (record.type == Type.COINBASE) {
                        if (!foundBlocks.contains(record.blockNumber)) {
                            foundBlocks.add(record.blockNumber);
                        } else {
                            deadBlocks.add(record.blockNumber);
                        }
                    }
                }
                record = recordInputStream.readRecord();
            }
        }

        averageExtrasPerBlock = (double)extraCount / (blockCount * Node.values().length);
        extrasPerTxRate = averageExtrasPerBlock / txRate;
    }
}
