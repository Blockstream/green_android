package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;

import java.io.IOException;
import java.util.List;
import java.util.Observable;

public class FeeObservable extends Observable {
    private List<Long> fees;
    private GDKSession mSession;
    private ListeningExecutorService mExecutor;


    public FeeObservable(final GDKSession session, final ListeningExecutorService executor) {
        mSession = session;
        mExecutor = executor;
        refresh();
    }

    public void refresh() {
        mExecutor.submit(() -> {
            try {
                final List<Long> feeEstimates = mSession.getFeeEstimates();
                setFees(feeEstimates);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public List<Long> getFees() {
        return fees;
    }

    public void setFees(final List<Long> fees) {
        Log.d("OBS", "setFees(" +  fees + ")");
        this.fees = fees;
        setChanged();
        notifyObservers();
    }
}
