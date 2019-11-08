package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.greenapi.data.BalanceData;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import static com.greenaddress.gdk.GDKSession.getSession;

public class BalanceDataObservable extends Observable implements Observer {
    private Map<String, Long> mSatoshiData;
    private ListeningExecutorService mExecutor;
    private Integer mSubaccount;

    public BalanceDataObservable(final ListeningExecutorService executor,
                                 final AssetsDataObservable assetsDataObservable,
                                 final Integer subaccount) {
        mExecutor = executor;
        mSubaccount = subaccount;

        assetsDataObservable.addObserver((observable, o) -> this.refresh());
    }

    public void refresh() {
        mExecutor.submit(() -> {
            try {
                final Map<String,Long> balance = getSession().getBalance(mSubaccount, 0);
                setBalanceData(balance);
            } catch (Exception e) {
                Log.e("OBS", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public Map<String, Long> getBalanceData() {
        if (mSatoshiData == null) {
            Log.e("OBS", "Balance observable does not hold the data yet, asking syncronously");
            try {
                mSatoshiData = getSession().getBalance(mSubaccount, 0);
            } catch (Exception e) {
                Log.e("OBS", e.getMessage());
                e.printStackTrace();
            }
        }
        return mSatoshiData;
    }

    public Long getBtcBalanceData() {
        return getBalanceData().get("btc");
    }

    public Integer getSubaccount() {
        return mSubaccount;
    }

    public void setBalanceData(final Map<String,Long> mSatoshiData) {
        Log.d("OBS", "setBalanceData(" +  mSubaccount + ", " + mSatoshiData + ")");
        this.mSatoshiData = mSatoshiData;
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
