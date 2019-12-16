package com.greenaddress.greenapi.model;

import android.util.Log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.greenapi.data.SettingsData;

import java.io.IOException;
import java.util.List;
import java.util.Observable;

import static com.greenaddress.gdk.GDKSession.getSession;

public class SettingsObservable extends Observable {
    private SettingsData mSettings;
    private ListeningExecutorService mExecutor;
    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    public SettingsData getSettings() {
        return mSettings;
    }

    public void setSettings(final SettingsData mSettings) {
        Log.d("OBS", "setSettings(" +  mSettings + ")");

        this.mSettings = mSettings;
        setChanged();
        notifyObservers();
    }

    public SettingsObservable(final ListeningExecutorService executor) {
        super();
        mExecutor = executor;
        refresh();
    }

    private void refresh() {
        try {
            final ObjectNode settings = getSession().getSettings();
            setSettings(mObjectMapper.convertValue(settings, SettingsData.class));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
