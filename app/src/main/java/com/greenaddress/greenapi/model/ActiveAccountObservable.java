package com.greenaddress.greenapi.model;

import android.util.Log;

import java.util.Observable;

public class ActiveAccountObservable extends Observable {
    private Integer activeAccount;

    public Integer getActiveAccount() {
        return activeAccount;
    }

    public void setActiveAccount(final Integer activeAccount) {
        Log.d("OBS", "setActiveAccount(" + activeAccount + ")");
        this.activeAccount = activeAccount;
        setChanged();
        notifyObservers();
    }
}
