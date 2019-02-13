package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.PagedData;
import com.greenaddress.greenapi.data.TransactionData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class TransactionDataObservable extends Observable implements Observer {
    private List<TransactionData> mTransactionDataList = new ArrayList<>();
    private GDKSession mSession;
    private ListeningExecutorService mExecutor;
    private Integer mSubaccount;
    private boolean mUTXOOnly;
    private Integer mNextPage;
    private Integer mPageLoaded = 0;
    private boolean mExecutedOnce = false;

    private TransactionDataObservable() {}

    public TransactionDataObservable(final GDKSession session, final ListeningExecutorService executor,
                                     final Integer subaccount, final boolean UTXOOnly) {
        mExecutor = executor;
        mSession = session;
        mSubaccount = subaccount;
        mUTXOOnly = UTXOOnly;
        // this is not initialized by default but by visiting the account detail page
    }

    public void refresh() {
        refresh(true);
    }

    public void refresh(final boolean reset) {
        if (reset) {
            mNextPage = null;
            mPageLoaded = 0;
            mTransactionDataList.clear();
        }
        mExecutor.submit(this::refreshSync);
    }

    public synchronized void refreshSync() {
        try {
            List<TransactionData> transactions;
            if (mUTXOOnly) {
                transactions = mSession.getUTXO(mSubaccount, 0);
            } else {
                PagedData<TransactionData> transactionsPaged = mSession.getTransactionsPaged(mSubaccount, mNextPage == null ? 0 : mNextPage);
                Log.d("OBS","page " + transactionsPaged.getPageId() +
                        "nextpage " + transactionsPaged.getNextPageId() );
                transactions = transactionsPaged.getList();
                mNextPage = transactionsPaged.getNextPageId();
                mPageLoaded ++;
            }
            mExecutedOnce = true;
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
        Log.d("OBS", "pageLoaded:" + mPageLoaded + " mNextPage: " + mNextPage + " set" + (mUTXOOnly ? "UTXO" : "Transaction") + "DataList(" +  mSubaccount + ", " + transactionData + ")");
        this.mTransactionDataList.addAll(transactionData);
        fire();
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof ActiveAccountObservable) {
            if ( ((ActiveAccountObservable) observable).getActiveAccount().equals(mSubaccount))
                if (!isExecutedOnce())
                    refresh();

        } else if (observable instanceof BlockchainHeightObservable) {
            if (isExecutedOnce()) {
                refresh();
            }
        }
    }

    public void fire() {
        setChanged();
        notifyObservers();
    }

    public boolean isLastPage() {
        return mNextPage != null && mNextPage == 0;
    }

    public boolean isExecutedOnce() {
        return mExecutedOnce;
    }
}
