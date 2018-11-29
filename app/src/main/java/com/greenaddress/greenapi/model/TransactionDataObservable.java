package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.TransactionData;

import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class TransactionDataObservable extends Observable implements Observer {
    private List<TransactionData> mTransactionDataList;
    private GDKSession mSession;
    private ListeningExecutorService mExecutor;
    private Integer mSubaccount;
    private boolean mUTXOOnly;

    private TransactionDataObservable() {}

    public TransactionDataObservable(final GDKSession session, final ListeningExecutorService executor,
                                     final Integer subaccount, final boolean UTXOOnly) {
        mExecutor = executor;
        mSession = session;
        mSubaccount = subaccount;
        mUTXOOnly = UTXOOnly;
        // this is not initializied by default but by visiting the account detail page
    }

    public void refresh() {
        mExecutor.submit(() -> {
            refreshSync();
        });
    }

    public void refreshSync() {
        try {
            List<TransactionData> transactions;
            if (mUTXOOnly) {
                transactions = mSession.getUTXO(mSubaccount, 0);
            } else {
                transactions = mSession.getTransactions(mSubaccount, 0);
            }
            setTransactionDataList(transactions);
        } catch (IOException e) {
            Log.e("OBS", e.getMessage());
            e.printStackTrace();
        }
    }

    public List<TransactionData> getTransactionDataList() {
        return mTransactionDataList;
    }

    public Integer getSubaccount() {
        return mSubaccount;
    }

    public void setTransactionDataList(final List<TransactionData> transactionData) {
        Log.d("OBS",
              "set" + (mUTXOOnly ? "UTXO" : "Transaction") + "DataList(" +  mSubaccount + ", " + transactionData + ")");
        this.mTransactionDataList = transactionData;
        fire();
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof ActiveAccountObservable) {
            if ( ((ActiveAccountObservable) observable).getActiveAccount().equals(mSubaccount))
                refresh();
        } else if (observable instanceof BlockchainHeightObservable) {
            if (mTransactionDataList != null) {
                refresh();
            }
        }
    }

    public void fire() {
        setChanged();
        notifyObservers();
    }
}
