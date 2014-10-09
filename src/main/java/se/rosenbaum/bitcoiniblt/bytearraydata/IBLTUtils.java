package se.rosenbaum.bitcoiniblt.bytearraydata;

import se.rosenbaum.iblt.Cell;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.ByteArrayData;
import se.rosenbaum.iblt.data.LongData;
import se.rosenbaum.iblt.hash.*;

public class IBLTUtils {

    public static Cell<ByteArrayData, ByteArrayData>[] createCells(int numberOfCells) {
        Cell[] cells = new Cell[numberOfCells];
        HashFunction hashFunction = new ByteArrayDataHashFunction(4);
        for (int i = 0; i < numberOfCells; i++) {
            cells[i] = new Cell(data(8), data(8), data(4), hashFunction);
        }
        return cells;
    }

    public static ByteArrayData data(int size) {
        return new ByteArrayData(size);
    }

    public static IBLT<ByteArrayData, ByteArrayData> createIblt(int cellCount, int hashFunctionCount) {
        Cell<ByteArrayData, ByteArrayData>[] cells = createCells(cellCount);
        ByteArraySubtablesHashFunctions hashFunctions = new ByteArraySubtablesHashFunctions(cellCount, hashFunctionCount);
        return new IBLT<ByteArrayData, ByteArrayData>(cells, hashFunctions);
    }
}
