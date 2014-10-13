package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.params.MainNetParams;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static org.junit.Assert.fail;

public class TransactionStatsTest extends ClientCoderTest {

    @Test
    public void testGetTransactionStats() throws IOException {
        File csvFile = new File(System.getProperty("java.io.tmpdir"), "transactionStats.csv");
        Writer writer = new FileWriter(csvFile);

        processTransactions(MAINNET_BLOCK, 10000, new SizePrintingProcessor(writer));
        writer.close();
    }

    private static class SizePrintingProcessor implements TransactionProcessor {
        private Writer writer;
        public SizePrintingProcessor(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void process(Transaction transaction) {
            try {
                writer.write(transaction.bitcoinSerialize().length + "," + transaction.getInputs().size() +
                        "," + transaction.getOutputs().size() + "," + transaction.getHash().toString() + "\n");
            } catch (IOException e) {
                fail();
            }
        }
    }

    public NetworkParameters getParams() {
        return MainNetParams.get();
    }
}
