package com.greenaddress.greenapi.model;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import static com.greenaddress.gdk.GDKSession.getSession;

public class AssetsDataObservable extends Observable {
    private final Map<String, Bitmap> mIcons = new HashMap<>();
    private boolean mAssetsLoaded = false;
    private boolean mShownErrorPopup = false;

    private final ListeningExecutorService mExecutor;

    public AssetsDataObservable(final ListeningExecutorService executor) {
        mExecutor = executor;
    }

    private void doRefresh() {
        try {
            // try from cache first
            setAssetsIcons(getSession().getAssetsIcons(Integer.MAX_VALUE));
        } catch (final RuntimeException e) {
            // we let this one fail and retry on the next one
            Log.e("ASSETS_OBS CACHED REQ:", e.toString());
        }

        try {
            // then refresh the cache if older than 24h
            setAssetsIcons(getSession().getAssetsIcons(24 * 60 * 60));
        } catch (final RuntimeException e) {
            Log.e("ASSETS_OBS REFRESH REQ", e.toString());
            e.printStackTrace();

            // report the error
            if (!mAssetsLoaded) {
                notifyRegistryError();
            }
        }
    }

    public void refresh() {
        // run doRefresh asynchronously
        mExecutor.submit(this::doRefresh);
    }

    public Map<String, Bitmap> getAssetsIcons() {
        return mIcons;
    }

    public boolean isAssetsLoaded() {
        return mAssetsLoaded;
    }

    public synchronized boolean isShownErrorPopup() {
        return mShownErrorPopup;
    }

    public synchronized void setShownErrorPopup() {
        this.mShownErrorPopup = true;
    }

    private void setAssetsIcons(final Map<String, Bitmap> icons) {
        Log.d("ASSETS_OBS", "setAssetsIcons(" +  icons + ")");
        mIcons.clear(); // the most updated response comes later, so we clear the older data
        mIcons.putAll(icons);
        mAssetsLoaded = true;
        setChanged();
        notifyObservers();
    }

    private void notifyRegistryError() {
        Log.w("ASSETS_OBS", "notifying observer about a registry failure");

        setChanged();
        notifyObservers();
    }
}
