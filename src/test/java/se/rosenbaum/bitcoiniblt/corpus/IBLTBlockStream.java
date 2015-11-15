package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Sha256Hash;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class IBLTBlockStream {
    RecordInputStream[] recordStreams;

    public IBLTBlockStream(File corpusDirectory, CorpusData.Node... nodes) throws IOException {
        CorpusData corpusData = new CorpusData(corpusDirectory);
        recordStreams = new RecordInputStream[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            recordStreams[i] = corpusData.getRecordInputStream(nodes[i]);
        }
    }


}
