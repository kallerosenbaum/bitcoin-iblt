package se.rosenbaum.bitcoiniblt.corpus;

/**
 * User: kalle
 * Date: 2015-09-23 19:29
 */
enum Type {
    INCOMING_TX(1), COINBASE(2), UNKNOWN(3), KNOWN(4), MOMPOOL_ONLY(5);

    private int number;

    private Type(int number) {
        this.number = number;
    }

    static Type of(int number) {
        for (Type type : Type.values()) {
            if (type.number == number) {
                return type;
            }
        }
        throw new RuntimeException("Unknown type " + number);
    }
}
