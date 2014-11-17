package se.rosenbaum.bitcoiniblt.bytearraydata;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.TransactionCoder;
import se.rosenbaum.iblt.data.ByteArrayData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ByteArrayDataTransactionCoder implements TransactionCoder<ByteArrayData, ByteArrayData> {
    NetworkParameters params;
    byte[] salt;
    private int keySize;
    private int valueSize;
    private Map<ByteArrayData, Map<ByteArrayData, ByteArrayData>> encodedTransactions = new HashMap<ByteArrayData, Map<ByteArrayData, ByteArrayData>>();

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

        ByteArrayKeyData key = getKeyData(transaction);
        encodedTransactions.put(new ByteArrayData(key.getBytes()), map);
        char keyCounter = 0; // char is a 16 bit unsigned integer

        byte[] bytes = transaction.bitcoinSerialize();
        for (int i = 0; i < bytes.length; i += valueSize) {
            key.setIndex(keyCounter++);
            ByteArrayData keyData = new ByteArrayData(key.getBytes());
            ByteArrayData valueData = new ByteArrayData(Arrays.copyOfRange(bytes, i, i+valueSize));
            map.put(keyData, valueData);
        }

        return map;
    }

    public ByteArrayKeyData getKeyData(Transaction transaction) {
        byte[] transactionId = transaction.getHash().getBytes();
        byte[] key = Arrays.copyOf(transactionId, transactionId.length + salt.length);
        System.arraycopy(salt, 0, key, transactionId.length, salt.length);
        key = Sha256Hash.create(key).getBytes();

        byte[] data = new byte[keySize];
        // 64 first bits (the last two bytes will be overwritten by counter
        System.arraycopy(key, 0, data, 0, keySize - 2);
        return new ByteArrayKeyData(data);
    }

    @Override
    public List<Transaction> decodeTransactions(Map<ByteArrayData, ByteArrayData> entries) {
        Map<ByteArrayData, byte[]> transactionGroups = new HashMap<ByteArrayData, byte[]>();
        int i = 0;
        for (Map.Entry<ByteArrayData, ByteArrayData> entry : entries.entrySet()) {
            ByteArrayKeyData key = new ByteArrayKeyData(entry.getKey().getValue());
            char keyCounter = key.getIndex();

            ByteArrayKeyData groupKey = new ByteArrayKeyData(key.getBytes());
            groupKey.setIndex((char)0);
            ByteArrayData groupData = new ByteArrayData(groupKey.getBytes());

            byte[] value = entry.getValue().getValue();

            byte[] txBytes = transactionGroups.get(groupData);
            if (txBytes == null) {
                txBytes = new byte[(keyCounter+1) * valueSize];
                transactionGroups.put(groupData, txBytes);
            } else if (txBytes.length < (keyCounter+1) * valueSize) {
                byte[] newTxBytes = new byte[(keyCounter+1) * valueSize];
                System.arraycopy(txBytes, 0, newTxBytes, 0, txBytes.length);
                transactionGroups.put(groupData, newTxBytes);
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

    @Override
    public Map<ByteArrayData, Map<ByteArrayData, ByteArrayData>> getEncodedTransactions() {
        return encodedTransactions;
    }
}
