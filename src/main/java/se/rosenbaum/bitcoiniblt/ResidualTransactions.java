package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Transaction;

import java.util.ArrayList;
import java.util.Collection;

public class ResidualTransactions {
    private Collection<Transaction> extraTransactions = new ArrayList<Transaction>();
    private Collection<Transaction> absentTransactions = new ArrayList<Transaction>();


    public ResidualTransactions(Collection<Transaction> extraTransactions, Collection<Transaction> absentTransactions) {
        this.extraTransactions = extraTransactions;
        this.absentTransactions = absentTransactions;
    }

    public Collection<Transaction> getExtraTransactions() {
        return extraTransactions;
    }

    public Collection<Transaction> getAbsentTransactions() {
        return absentTransactions;
    }
}
