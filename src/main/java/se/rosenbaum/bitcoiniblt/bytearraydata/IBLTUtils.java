package se.rosenbaum.bitcoiniblt.bytearraydata;

import se.rosenbaum.iblt.Cell;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.data.ByteArrayData;
import se.rosenbaum.iblt.hash.ByteArrayDataHashFunction;
import se.rosenbaum.iblt.hash.ByteArraySubtablesHashFunctions;
import se.rosenbaum.iblt.hash.HashFunction;

public class IBLTUtils {

    public Cell<ByteArrayData, ByteArrayData>[] createCells(int numberOfCells, int keySize, int valueSize, int hashSumSize) {
        Cell[] cells = new Cell[numberOfCells];
        HashFunction hashFunction = new ByteArrayDataHashFunction(hashSumSize);
        for (int i = 0; i < numberOfCells; i++) {
            cells[i] = new Cell(data(keySize), data(valueSize), data(hashSumSize), hashFunction, 0);
        }
        return cells;
    }

    public ByteArrayData data(int size) {
        return new ByteArrayData(size);
    }

    public IBLT<ByteArrayData, ByteArrayData> createIblt(int cellCount, int hashFunctionCount, int keySize, int valueSize, int hashSumSize) {
        Cell<ByteArrayData, ByteArrayData>[] cells = createCells(cellCount, keySize, valueSize, hashSumSize);
        ByteArraySubtablesHashFunctions<ByteArrayData> hashFunctions = new ByteArraySubtablesHashFunctions<ByteArrayData>(cellCount, hashFunctionCount);
        return new IBLT<ByteArrayData, ByteArrayData>(cells, hashFunctions);
    }
}
