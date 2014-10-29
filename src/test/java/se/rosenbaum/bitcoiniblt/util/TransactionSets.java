package se.rosenbaum.bitcoiniblt.util;

import org.bitcoinj.core.Transaction;

import java.util.List;

/**
* User: kalle
* Date: 10/29/14 8:34 PM
*/
public class TransactionSets {
    private List<Transaction> sendersTransactions;
    private List<Transaction> receiversTransactions;


    public List<Transaction> getSendersTransactions() {
        return sendersTransactions;
    }

    public void setSendersTransactions(List<Transaction> sendersTransactions) {
        this.sendersTransactions = sendersTransactions;
    }

    public List<Transaction> getReceiversTransactions() {
        return receiversTransactions;
    }

    public void setReceiversTransactions(List<Transaction> receiversTransactions) {
        this.receiversTransactions = receiversTransactions;
    }
}
