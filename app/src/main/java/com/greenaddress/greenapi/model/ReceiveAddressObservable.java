package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;

import java.util.Observable;
import java.util.Observer;

public class ReceiveAddressObservable extends Observable implements Observer {
    private String mReceiveAddress;
    private GDKSession mSession;
    private ListeningExecutorService mExecutor;
    private Integer mSubaccount;

    private ReceiveAddressObservable() {}

    public ReceiveAddressObservable(final GDKSession session, final ListeningExecutorService executor,
                                    final Integer subaccount) {
        mExecutor = executor;
        mSession = session;
        mSubaccount = subaccount;
    }

    public void refresh() {
        mExecutor.submit(() -> {
            final String address = mSession.getReceiveAddress(mSubaccount);
            setReceiveAddress(address);
        });
    }

    public String getReceiveAddress() {
        return mReceiveAddress;
    }

    public Integer getSubaccount() {
        return mSubaccount;
    }

    public void setReceiveAddress(final String receiveAddress) {
        Log.d("OBS", "setReceiveAddress(" +  mSubaccount + ", " + receiveAddress + ")");
        this.mReceiveAddress = receiveAddress;
        setChanged();
        notifyObservers();
    }

    @Override
    public void update(Observable observable, Object arg) {
        if (observable instanceof ActiveAccountObservable) {
            if ( ((ActiveAccountObservable) observable).getActiveAccount().equals(mSubaccount)
                    && mReceiveAddress == null)
                refresh();
        }
    }
}
