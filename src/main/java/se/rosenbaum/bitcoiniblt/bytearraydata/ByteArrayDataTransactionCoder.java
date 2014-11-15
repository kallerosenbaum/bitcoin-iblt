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

        KeyByteArrayData key = getKeyData(transaction);
        char keyCounter = 0; // char is a 16 bit unsigned integer

        byte[] bytes = transaction.bitcoinSerialize();
        for (int i = 0; i < bytes.length; i += valueSize) {
            key.setIndex(keyCounter++);
            ByteArrayData keyData = key.copy();
            ByteArrayData valueData = new ByteArrayData(Arrays.copyOfRange(bytes, i, i+valueSize));
            map.put(keyData, valueData);
        }
        return map;
    }

    public KeyByteArrayData getKeyData(Transaction transaction) {
        return new KeyByteArrayData(transaction, keySize, salt);
    }

    public List<Transaction> decodeTransactions(Map<ByteArrayData, ByteArrayData> entries) {
        Map<ByteArrayData, byte[]> transactionGroups = new HashMap<ByteArrayData, byte[]>();
        int i = 0;
        for (Map.Entry<ByteArrayData, ByteArrayData> entry : entries.entrySet()) {
            KeyByteArrayData key = new KeyByteArrayData(entry.getKey());
            char keyCounter = key.getIndexPart();

            ByteArrayData hashPart = key.getHashPart();
            byte[] value = entry.getValue().getValue();

            byte[] txBytes = transactionGroups.get(hashPart);
            if (txBytes == null) {
                txBytes = new byte[(keyCounter+1) * valueSize];
                transactionGroups.put(hashPart, txBytes);
            } else if (txBytes.length < (keyCounter+1) * valueSize) {
                byte[] newTxBytes = new byte[(keyCounter+1) * valueSize];
                System.arraycopy(txBytes, 0, newTxBytes, 0, txBytes.length);
                transactionGroups.put(hashPart, newTxBytes);
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
