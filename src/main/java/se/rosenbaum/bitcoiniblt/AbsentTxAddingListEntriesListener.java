package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.bytearraydata.KeyByteArrayData;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.ListEntriesListener;
import se.rosenbaum.iblt.data.ByteArrayData;

import java.util.List;
import java.util.Map;

public class AbsentTxAddingListEntriesListener implements ListEntriesListener<ByteArrayData, ByteArrayData> {
    IBLT<ByteArrayData, ByteArrayData> iblt;
    List<Transaction> myTransactions;
    TransactionCoder<ByteArrayData, ByteArrayData> transactionCoder;

    @Override
    public void absentKeyDetected(ByteArrayData key, ByteArrayData value) {
        KeyByteArrayData keyData = new KeyByteArrayData(key);
        keyData.setIndex((char)0);

        for (Transaction myTransaction : myTransactions) {
            Map<ByteArrayData,ByteArrayData> dataMap = transactionCoder.encodeTransaction(myTransaction);

        }
    }
}
