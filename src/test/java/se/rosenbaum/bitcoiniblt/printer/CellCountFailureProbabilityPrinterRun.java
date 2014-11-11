package se.rosenbaum.bitcoiniblt.printer;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import se.rosenbaum.bitcoiniblt.printer.FailureProbabilityPrinter.DataPoint;

public class CellCountFailureProbabilityPrinterRun {

    @Test
    public void testCreateImage() throws IOException {

        List<DataPoint> dataPoints = new ArrayList<DataPoint>();

        dataPoints.add(new DataPoint(100, 10000));
        dataPoints.add(new DataPoint(101, 9999));
        dataPoints.add(new DataPoint(200, 5000));
        dataPoints.add(new DataPoint(600, 4000));


        CellCountFailureProbabilityPrinter printer = new CellCountFailureProbabilityPrinter(new File("/tmp"));
        printer.createImage(dataPoints);
    }
}
