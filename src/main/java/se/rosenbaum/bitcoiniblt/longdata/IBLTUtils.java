package se.rosenbaum.bitcoiniblt.longdata;

import se.rosenbaum.iblt.Cell;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.LongData;
import se.rosenbaum.iblt.hash.LongDataHashFunction;
import se.rosenbaum.iblt.hash.LongDataSubtablesHashFunctions;

public class IBLTUtils {
    public static Cell<LongData, LongData>[] createCells(int numberOfCells) {
        Cell[] cells = new Cell[numberOfCells];
        LongDataHashFunction hashFunction = new LongDataHashFunction();
        for (int i = 0; i < numberOfCells; i++) {
            cells[i] = new Cell(data(0), data(0), data(0), hashFunction, 0);
        }
        return cells;
    }

    public static LongData data(long value) {
        return new LongData(value);
    }
    
    public static IBLT<LongData, LongData> createIblt(int cellCount, int hashFunctionCount) {
        Cell<LongData, LongData>[] cells = createCells(cellCount);
        LongDataSubtablesHashFunctions hashFunctions = new LongDataSubtablesHashFunctions(cellCount, hashFunctionCount);
        return new IBLT<LongData, LongData>(cells, hashFunctions);
    }

}
