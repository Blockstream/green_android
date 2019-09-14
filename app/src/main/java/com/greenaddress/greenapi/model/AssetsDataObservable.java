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
    private final ListeningExecutorService mExecutor;

    public AssetsDataObservable(final ListeningExecutorService executor) {
        mExecutor = executor;
    }

    public void refresh() {
        mExecutor.submit(() -> {
            try {
                setAssetsIcons(getSession().getAssetsIcons());
            } catch (final RuntimeException e) {
                Log.e("OBS", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public Map<String, Bitmap> getAssetsIcons() {
        return mIcons;
    }

    public void setAssetsIcons(final Map<String, Bitmap> icons) {
        Log.d("OBS", "setAssetsIcons(" +  icons + ")");
        mIcons.clear();
        mIcons.putAll(icons);
        setChanged();
        notifyObservers();
    }
}
