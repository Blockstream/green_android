package com.greenaddress.greenapi.model;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.gdk.GDKTwoFactorCall;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import static com.greenaddress.gdk.GDKSession.getSession;

public class BalanceDataObservable extends Observable implements Observer {
    private Map<String, Long> mSatoshiData;
    private ListeningExecutorService mExecutor;
    private CodeResolver mCodeResolver;
    private Integer mSubaccount;

    public BalanceDataObservable(final ListeningExecutorService executor,
                                 final CodeResolver codeResolver,
                                 final AssetsDataObservable assetsDataObservable,
                                 final Integer subaccount) {
        mExecutor = executor;
        mCodeResolver = codeResolver;
        mSubaccount = subaccount;

        assetsDataObservable.addObserver((observable, o) -> this.refresh());
    }

    private Map<String, Long> processResponse(final ObjectNode balanceData) throws JsonProcessingException {
        final Map<String, Long> map = new HashMap<>();
        final Iterator<String> iterator = balanceData.fieldNames();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            map.put(key, balanceData.get(key).asLong(0));
        }
        return map;
    }

    public void refresh() {
        mExecutor.submit(() -> {
            try {
                final GDKTwoFactorCall call = getSession().getBalance(null, mSubaccount, 0);
                final ObjectNode response = call.resolve(null, mCodeResolver);
                final Map<String,Long> balance = processResponse(response);
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
                final GDKTwoFactorCall call = getSession().getBalance(null, mSubaccount, 0);
                final ObjectNode response = call.resolve(null, mCodeResolver);
                mSatoshiData = processResponse(response);
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
