package se.rosenbaum.bitcoiniblt.util;

public class BlockStatsResult {
    private boolean success;
    private long encodingTime;
    private long decodingTime;

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
}
