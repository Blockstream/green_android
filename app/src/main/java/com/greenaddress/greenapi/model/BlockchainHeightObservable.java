package com.greenaddress.greenapi.model;

import android.util.Log;

import java.util.Observable;

public class BlockchainHeightObservable extends Observable {
    private Integer height;

    public Integer getHeight() {
        return height;
    }

    public void setHeight(final Integer height) {
        Log.d("OBS", "setHeight(" +  height + ")");
        this.height = height;
        setChanged();
        notifyObservers();
    }
}
