package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.List;
import java.util.Observable;

import static com.greenaddress.gdk.GDKSession.getSession;

public class FeeObservable extends Observable {
    private List<Long> fees;
    private ListeningExecutorService mExecutor;


    public FeeObservable(final ListeningExecutorService executor) {
        mExecutor = executor;
        refresh();
    }

    public void refresh() {

        try {
            final List<Long> feeEstimates = getSession().getFeeEstimates();
            setFees(feeEstimates);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
