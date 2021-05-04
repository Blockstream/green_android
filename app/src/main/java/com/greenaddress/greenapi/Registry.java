package com.greenaddress.greenapi;

import com.blockstream.gdk.AssetManager;
import com.blockstream.gdk.data.Asset;
import com.greenaddress.greenapi.data.AssetInfoData;

import javax.annotation.Nullable;


@Deprecated
public class Registry {
    private static Registry mIstance;
    private final AssetManager mAssetManager;

    public static Registry getInstance(){
        return mIstance;
    }

    public static void init(AssetManager assetManager){
        if(mIstance == null){
            mIstance = new Registry(assetManager);
        }
    }

    Registry(AssetManager assetManager) {
        mAssetManager = assetManager;
    }

    public AssetManager getAssetManager() { return mAssetManager; }

    @Nullable
    public AssetInfoData getAssetInfo(String assetId) {
         Asset asset = mAssetManager.getAsset(assetId);
         if(asset != null){
             return new AssetInfoData(asset.getAssetId(), asset.getName(), asset.getPrecision(), asset.getTicker(), asset.getEntity().getDomain());
         }
        return null;
    }
}
