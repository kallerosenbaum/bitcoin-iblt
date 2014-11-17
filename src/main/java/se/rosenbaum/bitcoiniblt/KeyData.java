package se.rosenbaum.bitcoiniblt;

import se.rosenbaum.iblt.data.Data;

public interface KeyData<D extends Data> {
    void setIndex(char index);

    char getIndexPart();

    D getData();
}
