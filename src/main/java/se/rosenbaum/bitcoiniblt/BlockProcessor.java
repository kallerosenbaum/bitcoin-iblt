package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Block;

public interface BlockProcessor {
    void process(Block block);
}
