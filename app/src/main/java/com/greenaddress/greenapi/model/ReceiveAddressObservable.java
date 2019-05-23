package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import static com.greenaddress.gdk.GDKSession.getSession;

import java.util.Observable;
import java.util.Observer;

public class ReceiveAddressObservable extends Observable implements Observer {
    private String mReceiveAddress;
    private ListeningExecutorService mExecutor;
    private Integer mSubaccount;

    private ReceiveAddressObservable() {}

    public ReceiveAddressObservable(final ListeningExecutorService executor,
                                    final Integer subaccount) {
        mExecutor = executor;
        mSubaccount = subaccount;
    }

    public void refresh() {
        mExecutor.submit(() -> {
            try {
                final String address = getSession().getReceiveAddress(mSubaccount);
                setReceiveAddress(address);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        if (mReceiveAddress == null && observable instanceof ActiveAccountObservable
            && ((ActiveAccountObservable) observable).getActiveAccount().equals(mSubaccount))
            refresh();
    }
}
