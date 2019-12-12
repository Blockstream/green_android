package com.greenaddress.greenapi.model;

import android.util.Log;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.greenapi.data.TwoFactorConfigData;

import java.util.Observable;

import static com.greenaddress.gdk.GDKSession.getSession;

public class TwoFactorConfigDataObservable extends Observable {
    private TwoFactorConfigData mTwoFactorConfigData;
    private ListeningExecutorService mExecutor;

    public TwoFactorConfigDataObservable(final ListeningExecutorService executor,
                                         final EventDataObservable eventDataObservable) {
        mExecutor = executor;
        addObserver(eventDataObservable);
        refresh();
    }

    public void refresh() {
        mExecutor.submit(() -> {
            try {
                final TwoFactorConfigData twoFactorConfig = getSession().getTwoFactorConfig();
                setTwoFactorConfigData(twoFactorConfig);
            } catch (Exception e) {
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
