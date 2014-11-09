package se.rosenbaum.bitcoiniblt.util;

public class BlockStatsResult {
    private boolean success;
    private long encodingTime;
    private long decodingTime;
    private int totalKeysCount;
    private int residualKeysCount;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getEncodingTime() {
        return encodingTime;
    }

    public void setEncodingTime(long encodingTime) {
        this.encodingTime = encodingTime;
    }

    public long getDecodingTime() {
        return decodingTime;
    }

    public void setDecodingTime(long decodingTime) {
        this.decodingTime = decodingTime;
    }

    public int getTotalKeysCount() {
        return totalKeysCount;
    }

    public void setTotalKeysCount(int totalKeysCount) {
        this.totalKeysCount = totalKeysCount;
    }

    public int getResidualKeysCount() {
        return residualKeysCount;
    }

    public void setResidualKeysCount(int residualKeysCount) {
        this.residualKeysCount = residualKeysCount;
    }
}
