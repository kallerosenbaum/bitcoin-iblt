package se.rosenbaum.bitcoiniblt;

import se.rosenbaum.bitcoiniblt.bytearraydata.ByteArrayKeyData;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.ListEntriesListener;
import se.rosenbaum.iblt.data.ByteArrayData;
import se.rosenbaum.iblt.data.Data;

import java.util.HashMap;
import java.util.Map;

public class AbsentTxAddingListEntriesListener<V extends Data> implements ListEntriesListener<ByteArrayData, V> {
    IBLT<ByteArrayData, ByteArrayData> iblt;
    Map<ByteArrayData, Map<ByteArrayData, ByteArrayData>> encodedTransactions;
    Map<ByteArrayData, ByteArrayData> absentEntries = new HashMap<ByteArrayData, ByteArrayData>();

    public AbsentTxAddingListEntriesListener(IBLT<ByteArrayData, ByteArrayData> iblt, Map<ByteArrayData, Map<ByteArrayData, ByteArrayData>> encodedTransactions) {
        this.encodedTransactions = encodedTransactions;
        this.iblt = iblt;
    }


    @Override
    public void absentKeyDetected(ByteArrayData key, V value) {
        ByteArrayKeyData keyData = new ByteArrayKeyData(key.getValue());
        keyData.setIndex((char)0);
        Map<ByteArrayData, ByteArrayData> fractions = encodedTransactions.get(new ByteArrayData(keyData.getBytes()));
        for (Map.Entry<ByteArrayData, ByteArrayData> dataEntry : fractions.entrySet()) {
            ByteArrayData key1 = dataEntry.getKey();
            ByteArrayData value1 = dataEntry.getValue();
            absentEntries.put(key1, value1);
            if (key1.equals(key)) {
                continue; // This key is already added to the IBLT
            }
            iblt.insert(key1, value1);
        }
    }

    public Map<ByteArrayData, ByteArrayData> getAbsentEntries() {
        return absentEntries;
    }
}
