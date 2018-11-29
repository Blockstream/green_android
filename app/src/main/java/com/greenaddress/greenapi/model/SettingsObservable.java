package com.greenaddress.greenapi.model;

import android.util.Log;

import com.greenaddress.greenapi.data.SettingsData;

import java.util.Observable;

public class SettingsObservable extends Observable {
    private SettingsData mSettings;

    public SettingsData getSettings() {
        return mSettings;
    }

    public void setSettings(final SettingsData mSettings) {
        Log.d("OBS", "setSettings(" +  mSettings + ")");

        this.mSettings = mSettings;
        setChanged();
        notifyObservers();
    }
}
