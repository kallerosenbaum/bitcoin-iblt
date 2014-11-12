package se.rosenbaum.bitcoiniblt.printer;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class OnePercentLineFailureProbabilityPrinterManualTest {

    @Test
    public void updateOldFileVersions() throws IOException {
        String tmpDir = System.getProperty("iblt.output.dir", ".");
        File tempDirectory = new File(new File(tmpDir), "data");
        OnePercentLineFailureProbabilityPrinter printer = new OnePercentLineFailureProbabilityPrinter(tempDirectory);
        printer.parseCsv(new File(tempDirectory, "onePercentLine-Stats20141110-2046.csv"));
        printer.finish();
    }
}
