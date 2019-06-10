package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import static com.greenaddress.gdk.GDKSession.getSession;

import com.greenaddress.greenapi.data.TransactionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class TransactionDataObservable extends Observable implements Observer {
    private List<TransactionData> mTransactionDataList = new ArrayList<>();
    private ListeningExecutorService mExecutor;
    private Integer mSubaccount;
    private boolean mUTXOOnly;
    private Integer mPageLoaded = 0;
    private Integer mLastPage = Integer.MAX_VALUE;
    private boolean mExecutedOnce = false;

    private static final int TX_PER_PAGE = 15;
    private TransactionDataObservable() {}

    public TransactionDataObservable(final ListeningExecutorService executor,
                                     final Integer subaccount, final boolean UTXOOnly) {
        mExecutor = executor;
        mSubaccount = subaccount;
        mUTXOOnly = UTXOOnly;
        // this is not initialized by default but by visiting the account detail page
    }

    public void refresh() {
        refresh(true);
    }

    public void refresh(final boolean reset) {
        if (reset) {
            mPageLoaded = 0;
            mLastPage = Integer.MAX_VALUE;
            mTransactionDataList.clear();
        }
        mExecutor.submit(this::refreshSync);
    }

    public synchronized void refreshSync() {
        try {
            List<TransactionData> transactions;
            if (mUTXOOnly) {
                transactions = getSession().getUTXO(mSubaccount, 0);
            } else {
                transactions = getSession().getTransactions(mSubaccount, mPageLoaded * TX_PER_PAGE, TX_PER_PAGE);
                if (transactions.size() < TX_PER_PAGE)
                    mLastPage = mPageLoaded;
                mPageLoaded++;
                Log.d("OBS", "page loaded " + mPageLoaded + " " + transactions.size() + " Loaded txs");
            }
            mExecutedOnce = true;
            setTransactionDataList(transactions);
        } catch (Exception e) {
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
              "pageLoaded:" + mPageLoaded + " set" + (mUTXOOnly ? "UTXO" : "Transaction") + "DataList(" +  mSubaccount + ", " + transactionData +
              ")");
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
        return (mPageLoaded >= mLastPage);
    }

    public boolean isExecutedOnce() {
        return mExecutedOnce;
    }
}
