package se.rosenbaum.bitcoiniblt.util;

/**
* User: kalle
* Date: 10/29/14 9:29 PM
*/
public class Interval {
    private int low;
    private int high = Integer.MAX_VALUE;

    public Interval(int low, int high) {
        this.setLow(low);
        this.setHigh(high);
    }

    public int nextValue(TestConfig config) {
        int next = getHigh() - (getHigh() - getLow())/2;
        // Must be a multiple of hashFunctionCount;

        return next - next % config.getHashFunctionCount();
    }
    public boolean isInsideInterval(int value) {
        return value < getHigh() && value > getLow();
    }

    public int getLow() {
        return low;
    }

    public void setLow(int low) {
        this.low = low;
    }

    public int getHigh() {
        return high;
    }

    public void setHigh(int high) {
        this.high = high;
    }
}
