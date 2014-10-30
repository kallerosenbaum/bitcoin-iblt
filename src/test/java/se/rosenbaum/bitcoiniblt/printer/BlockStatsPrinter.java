package se.rosenbaum.bitcoiniblt.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.chart.BarChart;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class BlockStatsPrinter {
    Logger logger = LoggerFactory.getLogger(BlockStatsPrinter.class);
    PrintWriter writer;
    private File tempDirectory;

    String dateString;

    public BlockStatsPrinter(File tempDirectory) throws IOException {
        this.tempDirectory = tempDirectory;
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHMM");
        dateString = dateFormat.format(new Date());
        writer = new PrintWriter(new FileWriter(getFile(".csv")));
    }

    // Suffix example: ".csv"
    public File getFile(String suffix) {
        String fileName = filePrefix() + dateString + suffix;
        return new File(tempDirectory, fileName);
    }

    public void finish() throws IOException {
        writer.close();
    }

    public File getTempDirectory() {
        return tempDirectory;
    }

    protected abstract String filePrefix();
}
