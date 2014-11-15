package se.rosenbaum.bitcoiniblt;

import se.rosenbaum.iblt.data.ByteArrayData;
import se.rosenbaum.iblt.data.Data;

public interface KeyData<D extends Data> extends Data<D> {
    void setIndex(char index);

    D getHashPart();

    char getIndexPart();
}
