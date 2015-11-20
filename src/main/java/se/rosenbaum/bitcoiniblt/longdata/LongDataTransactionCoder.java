package se.rosenbaum.bitcoiniblt.longdata;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.TransactionCoder;
import se.rosenbaum.iblt.data.LongData;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LongDataTransactionCoder implements TransactionCoder<LongData, LongData> {
    NetworkParameters params;
    byte[] salt;

    public LongDataTransactionCoder(NetworkParameters params, byte[] salt) {
        if (salt.length != 256/8) {
            throw new RuntimeException("Bad salt length! Must be 32, but was  " + salt.length);
        }

        this.params = params;
        this.salt = salt;
    }

    public Map<LongData, LongData> encodeTransaction(Transaction transaction) {
        Map<LongData, LongData> map = new HashMap<LongData, LongData>();
        byte[] transactionId = transaction.getHash().getBytes();
        byte[] key = Arrays.copyOf(transactionId, transactionId.length + salt.length);
        System.arraycopy(salt, 0, key, transactionId.length, salt.length);
        key = Sha256Hash.create(key).getBytes();

        byte[] keyBytes = Arrays.copyOfRange(key, 0, 8); // 64 first bits (the last to bytes will be overwritten by counter
        char keyCounter = 0; // char is a 16 bit unsigned integer

        byte[] bytes = transaction.bitcoinSerialize();
        for (int i = 0; i < bytes.length; i += 8) {
            byte[] keyCounterBytes = ByteBuffer.allocate(2).putChar(keyCounter++).array();
            keyBytes[6] = keyCounterBytes[0];
            keyBytes[7] = keyCounterBytes[1];

            LongData keyData = new LongData(ByteBuffer.wrap(keyBytes).getLong());
            LongData valueData = new LongData(ByteBuffer.wrap(Arrays.copyOfRange(bytes, i, i+8)).getLong());
            map.put(keyData, valueData);
        }
        return map;
    }

    public Transaction decodeTransaction(Map<LongData, LongData> map) {
        byte[] txBytes = new byte[map.size() * 8];

        for (Map.Entry<LongData, LongData> entry : map.entrySet()) {
            long key = entry.getKey().getValue();
            byte[] keyBytes = ByteBuffer.allocate(8).putLong(key).array();
            char keyCounter = ByteBuffer.wrap(keyBytes, 6, 2).getChar();

            long value = entry.getValue().getValue();
            byte[] valueBytes = ByteBuffer.allocate(8).putLong(value).array();

            System.arraycopy(valueBytes, 0, txBytes, keyCounter * 8, 8);
        }

        return new Transaction(params, txBytes);
    }

    public List<Transaction> decodeTransactions(Map<LongData, LongData> entries) {
        Map<Long, byte[]> transactionGroups = new HashMap<Long, byte[]>();
        for (Map.Entry<LongData, LongData> entry : entries.entrySet()) {
            long key = entry.getKey().getValue();
            byte[] keyBytes = ByteBuffer.allocate(8).putLong(key).array();
            char keyCounter = ByteBuffer.wrap(keyBytes, 6, 2).getChar();
            keyBytes[6] = 0;
            keyBytes[7] = 0;

            long keyKey = ByteBuffer.wrap(keyBytes).getLong();

            long value = entry.getValue().getValue();
            byte[] valueBytes = ByteBuffer.allocate(8).putLong(value).array();

            byte[] txBytes = transactionGroups.get(keyKey);
            if (txBytes == null) {
                txBytes = new byte[(keyCounter+1) * 8];
                transactionGroups.put(keyKey, txBytes);
            } else if (txBytes.length < (keyCounter+1) * 8) {
                byte[] newTxBytes = new byte[(keyCounter+1) * 8];
                System.arraycopy(txBytes, 0, newTxBytes, 0, txBytes.length);
                transactionGroups.put(keyKey, newTxBytes);
                txBytes = newTxBytes;
            }

            System.arraycopy(valueBytes, 0, txBytes, keyCounter * 8, 8);
        }
        List<Transaction> result = new ArrayList<Transaction>(transactionGroups.size());
        for (byte[] txBytes : transactionGroups.values()) {
            result.add(new Transaction(params, txBytes));
        }
        return result;
    }

    public Map<LongData, Map<LongData, LongData>> getEncodedTransactions() {
        // Unsupported
        return null;
    }

    public boolean isEncoded(Transaction transaction) {
        return false;
    }
}
