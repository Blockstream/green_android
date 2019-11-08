package com.greenaddress.greenapi.model;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.greenapi.data.AssetInfoData;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import static com.greenaddress.gdk.GDKSession.getSession;

public class AssetsDataObservable extends Observable {
    private final Map<String, Bitmap> mIcons = new HashMap<>();
    private final Map<String, AssetInfoData> mInfos = new HashMap<>();
    private boolean mAssetsLoaded = false;
    private boolean mShownErrorPopup = false;

    private final ListeningExecutorService mExecutor;

    public AssetsDataObservable(final ListeningExecutorService executor) {
        mExecutor = executor;
    }

    private void doRefresh() {
        try {
            // try from cache first
            final Map<String, Bitmap> icons = getSession().getAssetsIcons(false);
            final Map<String, AssetInfoData> infos = getSession().getAssetsInfos(false);
            setAssets(icons, infos);
        } catch (final Exception e) {
            // we let this one fail and retry on the next one
            Log.e("ASSETS_OBS CACHED REQ:", e.toString());
        }

        try {
            // then refresh the cache
            final Map<String, Bitmap> icons = getSession().getAssetsIcons(true);
            final Map<String, AssetInfoData> infos = getSession().getAssetsInfos(true);
            setAssets(icons, infos);
        } catch (final Exception e) {
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

    public Map<String, AssetInfoData> getAssetsInfos() {
        return mInfos;
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

    private void setAssets(final Map<String, Bitmap> icons, final Map<String, AssetInfoData> infos) {
        Log.d("ASSETS_OBS", "setAssetsIcons(" +  icons + ")");
        mInfos.clear();
        mIcons.clear();
        mInfos.putAll(infos);
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
