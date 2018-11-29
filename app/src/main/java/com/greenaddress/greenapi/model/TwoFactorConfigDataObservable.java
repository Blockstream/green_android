package com.greenaddress.greenapi.model;

import android.util.Log;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.TwoFactorConfigData;

import java.io.IOException;
import java.util.Observable;

public class TwoFactorConfigDataObservable extends Observable {
    private TwoFactorConfigData mTwoFactorConfigData;
    private GDKSession mSession;
    private ListeningExecutorService mExecutor;


    private TwoFactorConfigDataObservable() {}

    public TwoFactorConfigDataObservable(final GDKSession session,
                                         final ListeningExecutorService executor,
                                         final EventDataObservable eventDataObservable) {
        mSession = session;
        mExecutor = executor;
        addObserver(eventDataObservable);
        refresh();
    }

    public void refresh() {
        mExecutor.submit(() -> {
            try {
                final TwoFactorConfigData twoFactorConfig = mSession.getTwoFactorConfig();
                setTwoFactorConfigData(twoFactorConfig);
            } catch (IOException e) {
                Log.e("OBS", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void updateLimits(final ObjectNode newLimits) {
        mTwoFactorConfigData.setLimits(newLimits);
    }

    public TwoFactorConfigData getTwoFactorConfigData() {
        return mTwoFactorConfigData;
    }

    public void setTwoFactorConfigData(final TwoFactorConfigData mTwoFactorConfigData) {
        Log.d("OBS", "setTwoFactorConfigData(" +  mTwoFactorConfigData + ")");
        this.mTwoFactorConfigData = mTwoFactorConfigData;
        setChanged();
        notifyObservers();
    }
}
