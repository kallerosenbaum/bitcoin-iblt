package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Transaction;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class TestFilePrinter
{
    Writer writer;

    public TestFilePrinter(Writer writer)
    {
        this.writer = writer;
    }

    public void printTransactionSets(TransactionSets sets) throws IOException
    {
        writer.write("extra:");
        writeTransactions(sets.getSendersTransactions());
        writer.write("absent:");
        writeTransactions(sets.getReceiversTransactions());
    }

    private void writeTransactions(List<Transaction> transactions) throws IOException
    {
        boolean first = true;
        for (Transaction transaction : transactions)
        {
            if (!first)
            {
                writer.write(",");
            }
            first = false;
            writer.write(transaction.getHash().toString());
        }
        writer.write("\n");
    }

    public void writeTransactions(List<String> extra, List<String> absent)
    {
        try
        {
            writer.write("extra:");
            writeTransactionStrings(extra);
            writer.write("absent:");
            writeTransactionStrings(absent);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void writeTransactionStrings(List<String> transactions) throws IOException
    {
        boolean first = true;
        for (String transaction : transactions)
        {
            if (!first)
            {
                writer.write(",");
            }
            first = false;
            writer.write(transaction);
        }
        writer.write("\n");
    }
}
