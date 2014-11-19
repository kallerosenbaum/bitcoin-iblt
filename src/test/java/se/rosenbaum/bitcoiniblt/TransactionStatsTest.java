package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.util.TransactionProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;

import static org.junit.Assert.fail;

public class TransactionStatsTest extends ClientCoderTest {
    private static final Logger logger = LoggerFactory.getLogger(TransactionStatsTest.class);
    public static final int TRANSACTION_COUNT = 10000;
    public static final String FILE_PREFIX = "transactionStats";

    @Test
    public void testGetTransactionStats() throws IOException {
        File csvFile = new File(tempDirectory, FILE_PREFIX + ".csv");
        Writer writer = new FileWriter(csvFile);

        SizePrintingProcessor processor = new SizePrintingProcessor(writer);
        processTransactions(MAINNET_BLOCK, TRANSACTION_COUNT, processor);
        writer.close();
        printPercentiles(processor.transactionSizes);
    }

    @Test
    public void testAverageTransactionSize() throws IOException {
        final int[] totalSize = {0};
        int txCount = 10000;
        processTransactions("000000000000000015b798837e7bcc72362f6977faaf96c09347c63567f4f656", txCount,
                new TransactionProcessor() {
                    @Override
                    public void process(Transaction transaction) throws StopProcessingException {
                        totalSize[0] += transaction.bitcoinSerialize().length;
                    }
                }
        );
        System.out.println("Average transaction size: " + totalSize[0] / txCount);
    }


    @Test
    public void testCreateSortKey() throws IOException {
        final CanonicalOrderTransactionSorter sorter = new CanonicalOrderTransactionSorter();
        processTransactions(MAINNET_BLOCK, Integer.MAX_VALUE,
            new TransactionProcessor() {
                int txCount = 0;
                @Override
                public void process(Transaction transaction) throws StopProcessingException {
                    sorter.sortKey(transaction);
                    txCount++;
                    if (txCount % 1000 == 0) {
                        System.out.println("txCount=" + txCount);
                    }
                }
            }
        );
    }

    private void printPercentiles(int[] data) throws IOException {
        Arrays.sort(data);
        File file = new File(tempDirectory, FILE_PREFIX + "_percentiles.csv");
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        for (int i = 0; i < 10; i++) {
            int percentile = 10 * (i + 1);
            int percentileIndex = (data.length / 10) * (i+1) - 1;
            writer.printf("percentile %s: %s\n", percentile, data[percentileIndex]);
        }
        writer.close();
    }


    private static class SizePrintingProcessor implements TransactionProcessor {
        private Writer writer;
        int[] transactionSizes = new int[TRANSACTION_COUNT];
        int i = 0;

        public SizePrintingProcessor(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void process(Transaction transaction) {
            try {
                int length = transaction.bitcoinSerialize().length;
                writer.write(length + "," + transaction.getInputs().size() +
                        "," + transaction.getOutputs().size() + "," + transaction.getHash().toString() + "\n");
                transactionSizes[i++] = length;
            } catch (IOException e) {
                fail();
            }
        }
    }

    public NetworkParameters getParams() {
        return MainNetParams.get();
    }
}
