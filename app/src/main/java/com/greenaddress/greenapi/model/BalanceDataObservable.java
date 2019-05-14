package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.BalanceData;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class BalanceDataObservable extends Observable implements Observer {
    private BalanceData mBalanceData;
    private GDKSession mSession;
    private ListeningExecutorService mExecutor;
    private Integer mSubaccount;

    private BalanceDataObservable() {}

    public BalanceDataObservable(final GDKSession session, final ListeningExecutorService executor,
                                 final Integer subaccount) {
        mExecutor = executor;
        mSession = session;
        mSubaccount = subaccount;
    }

    public void refresh() {
        mExecutor.submit(() -> {
            try {
                final BalanceData balance = mSession.getBalance(mSubaccount, 0).get("btc");
                setBalanceData(balance);
            } catch (Exception e) {
                Log.e("OBS", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public BalanceData getBalanceData() {
        return mBalanceData;
    }

    public Integer getSubaccount() {
        return mSubaccount;
    }

    public void setBalanceData(final BalanceData mBalanceData) {
        Log.d("OBS", "setBalanceData(" +  mSubaccount + ", " + mBalanceData + ")");
        this.mBalanceData = mBalanceData;
        fire();
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof BlockchainHeightObservable) {
            refresh();
        }
    }

    public void fire() {
        setChanged();
        notifyObservers();
    }
}
