package se.rosenbaum.bitcoiniblt.util;

/**
* User: kalle
* Date: 10/29/14 8:26 PM
*/
public class TestConfig {
    private int txCount;
    private int extraTxCount;
    private int absentTxCount;
    private int hashFunctionCount;
    private int keySize;
    private int valueSize;
    private int keyHashSize;
    private int cellCount;
    private boolean randomTxSelection;

    public TestConfig(int txCount, int extraTxCount, int absentTxCount, int hashFunctionCount, int keySize,
                      int valueSize, int keyHashSize, int cellCount, boolean randomTxSelection) {
        this.setTxCount(txCount);
        this.setExtraTxCount(extraTxCount);
        this.setAbsentTxCount(absentTxCount);
        this.setHashFunctionCount(hashFunctionCount);
        this.setKeySize(keySize);
        this.setValueSize(valueSize);
        this.setKeyHashSize(keyHashSize);
        this.setCellCount(cellCount);
        this.setRandomTxSelection(randomTxSelection);
    }

    public TestConfig(TestConfig other) {
        this.setTxCount(other.txCount);
        this.setExtraTxCount(other.extraTxCount);
        this.setAbsentTxCount(other.absentTxCount);
        this.setHashFunctionCount(other.hashFunctionCount);
        this.setKeySize(other.keySize);
        this.setValueSize(other.valueSize);
        this.setKeyHashSize(other.keyHashSize);
        this.setCellCount(other.cellCount);
        this.setRandomTxSelection(other.randomTxSelection);
    }

    public int getIbltSize() {
        return getCellCount() * (getKeySize() + getValueSize() + getKeyHashSize() + 4/*counter*/);
    }

    public int getTxCount() {
        return txCount;
    }

    public void setTxCount(int txCount) {
        this.txCount = txCount;
    }

    public int getExtraTxCount() {
        return extraTxCount;
    }

    public void setExtraTxCount(int extraTxCount) {
        this.extraTxCount = extraTxCount;
    }

    public int getAbsentTxCount() {
        return absentTxCount;
    }

    public void setAbsentTxCount(int absentTxCount) {
        this.absentTxCount = absentTxCount;
    }

    public int getHashFunctionCount() {
        return hashFunctionCount;
    }

    public void setHashFunctionCount(int hashFunctionCount) {
        this.hashFunctionCount = hashFunctionCount;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public int getValueSize() {
        return valueSize;
    }

    public void setValueSize(int valueSize) {
        this.valueSize = valueSize;
    }

    public int getKeyHashSize() {
        return keyHashSize;
    }

    public void setKeyHashSize(int keyHashSize) {
        this.keyHashSize = keyHashSize;
    }

    public int getCellCount() {
        return cellCount;
    }

    public void setCellCount(int cellCount) {
        this.cellCount = cellCount;
    }

    public boolean isRandomTxSelection() {
        return randomTxSelection;
    }

    public void setRandomTxSelection(boolean randomTxSelection) {
        this.randomTxSelection = randomTxSelection;
    }
}
