package se.rosenbaum.bitcoiniblt.bytearraydata;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.NetworkParameters;
import se.rosenbaum.bitcoiniblt.TransactionCoder;
import se.rosenbaum.iblt.data.ByteArrayData;

import java.nio.ByteBuffer;
import java.util.*;

public class ByteArrayDataTransactionCoder implements TransactionCoder<ByteArrayData, ByteArrayData> {
    NetworkParameters params;
    byte[] salt;
    private int keySize;
    private int valueSize;

    public ByteArrayDataTransactionCoder(NetworkParameters params, byte[] salt, int keySize, int valueSize) {
        if (salt.length != 256/8) {
            throw new RuntimeException("Bad salt length! Must be 32, but was  " + salt.length);
        }

        this.params = params;
        this.salt = salt;
        this.keySize = keySize;
        this.valueSize = valueSize;
    }

    public Map<ByteArrayData, ByteArrayData> encodeTransaction(Transaction transaction) {
        Map<ByteArrayData, ByteArrayData> map = new HashMap<ByteArrayData, ByteArrayData>();
        byte[] transactionId = transaction.getHash().getBytes();
        byte[] key = Arrays.copyOf(transactionId, transactionId.length + salt.length);
        System.arraycopy(salt, 0, key, transactionId.length, salt.length);
        key = Sha256Hash.create(key).getBytes();

        byte[] keyBytes = Arrays.copyOfRange(key, 0, keySize); // 64 first bits (the last to bytes will be overwritten by counter
        char keyCounter = 0; // char is a 16 bit unsigned integer

        byte[] bytes = transaction.bitcoinSerialize();
        for (int i = 0; i < bytes.length; i += valueSize) {
            byte[] keyCounterBytes = ByteBuffer.allocate(2).putChar(keyCounter++).array();
            keyBytes[6] = keyCounterBytes[0];
            keyBytes[7] = keyCounterBytes[1];

            ByteArrayData keyData = new ByteArrayData(Arrays.copyOfRange(keyBytes, 0, keySize));
            ByteArrayData valueData = new ByteArrayData(Arrays.copyOfRange(bytes, i, i+valueSize));
            map.put(keyData, valueData);
        }
        return map;
    }

    public List<Transaction> decodeTransactions(Map<ByteArrayData, ByteArrayData> entries) {
        Map<ByteArrayData, byte[]> transactionGroups = new HashMap<ByteArrayData, byte[]>();
        int i = 0;
        for (Map.Entry<ByteArrayData, ByteArrayData> entry : entries.entrySet()) {
            byte[] key = entry.getKey().getValue();
            char keyCounter = ByteBuffer.wrap(key, keySize - 2, 2).getChar();

            ByteArrayData keyKey = new ByteArrayData(Arrays.copyOf(key, keySize - 2));
            byte[] value = entry.getValue().getValue();

            byte[] txBytes = transactionGroups.get(keyKey);
            if (txBytes == null) {
                txBytes = new byte[(keyCounter+1) * valueSize];
                transactionGroups.put(keyKey, txBytes);
            } else if (txBytes.length < (keyCounter+1) * valueSize) {
                byte[] newTxBytes = new byte[(keyCounter+1) * valueSize];
                System.arraycopy(txBytes, 0, newTxBytes, 0, txBytes.length);
                transactionGroups.put(keyKey, newTxBytes);
                txBytes = newTxBytes;
            }

            System.arraycopy(value, 0, txBytes, keyCounter * valueSize, valueSize);
        }
        List<Transaction> result = new ArrayList<Transaction>(transactionGroups.size());
        for (byte[] txBytes : transactionGroups.values()) {
            result.add(new Transaction(params, txBytes));
        }
        return result;
    }
}
