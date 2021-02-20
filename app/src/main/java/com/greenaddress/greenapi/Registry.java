package com.greenaddress.greenapi;

import android.graphics.Bitmap;
import android.util.Log;

import com.greenaddress.greenapi.data.AssetInfoData;

import java.util.HashMap;
import java.util.Map;


public class Registry {
    private final Map<String, Bitmap> mIcons = new HashMap<>();
    private final Map<String, AssetInfoData> mInfos = new HashMap<>();
    private final Session mSession;

    Registry(Session session) {
        mSession = session;
    }

    public void cached() {
        try {
            final Map<String, Bitmap> icons = mSession.getAssetsIcons(false);
            final Map<String, AssetInfoData> infos = mSession.getAssetsInfos(false);
            setAssets(icons, infos);
        } catch (final Exception e) {
            // we let this one fail and retry on the next one
            Log.e("ASSETS_OBS CACHED REQ:", e.toString());
        }
    }

    public void refresh() throws Exception {
        final Map<String, Bitmap> icons = mSession.getAssetsIcons(true);
        final Map<String, AssetInfoData> infos = mSession.getAssetsInfos(true);
        setAssets(icons, infos);
    }

    private void setAssets(final Map<String, Bitmap> icons, final Map<String, AssetInfoData> infos) {
        Log.d("ASSETS_OBS", "setAssetsIcons(" +  icons + ")");
        mInfos.clear();
        mIcons.clear();
        mInfos.putAll(infos);
        mIcons.putAll(icons);
    }

    public Map<String, Bitmap> getIcons() {
        return mIcons;
    }

    public Map<String, AssetInfoData> getInfos() {
        return mInfos;
    }
}
