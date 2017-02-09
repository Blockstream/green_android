package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LoginData {
    public final ArrayList<Map<String, ?>> mSubAccounts;
    public final Map<String, Object> mUserConfig;
    public final int[] mGaitPath;
    public final Map<String, ?> mRawData;
    public final Map<ByteBuffer, String> mAssets;
    public final Map<ByteBuffer, Integer> mAssetIds;
    public final Map<String, ByteBuffer> mAssetsByName;

    public LoginData(final Map<String, ?> map) {
        mSubAccounts = (ArrayList) map.get("subaccounts");
        mUserConfig = (Map<String, Object>) map.get("appearance");
        mGaitPath = getPath((String) map.get("gait_path"));
        mRawData = map;
        mAssets = new HashMap<>();
        mAssetIds = new HashMap<>();
        mAssetsByName = new HashMap<>();
        if (map.containsKey("assets")) {
            final Map<String, String> assets = (Map) map.get("assets");
            for (Map.Entry<String, String> entry : assets.entrySet()) {
                final ByteBuffer assetId = ByteBuffer.wrap(Wally.hex_to_bytes(entry.getKey()));
                mAssets.put(assetId, entry.getValue());
                mAssetsByName.put(entry.getValue(), assetId);
            }
        }
        if (map.containsKey("asset_ids")) {
            final Map<String, Integer> asset_ids = (Map) map.get("asset_ids");
            for (Map.Entry<String, Integer> entry : asset_ids.entrySet()) {
                final ByteBuffer assetId = ByteBuffer.wrap(Wally.hex_to_bytes(entry.getKey()));
                mAssetIds.put(assetId, entry.getValue());
            }
        }
    }

    public <T> T get(final String name) {
        return (T) mRawData.get(name);
    }

    final Integer getGAAssetId(final String assetName) {
        final ByteBuffer assetId = mAssetsByName.get(assetName);
        return assetId == null ? null : mAssetIds.get(assetId);
    }

    private int u8(final int i) { return i < 0 ? 256 + i : i; }

    private int[] getPath(final String pathHex) {
        if (pathHex == null)
            return null;
        final byte[] pathBytes = Wally.hex_to_bytes(pathHex);
        final int[] path = new int[32];
        for (int i = 0; i < 32; ++i)
            path[i] = u8(pathBytes[i * 2]) * 256 + u8(pathBytes[i * 2 + 1]);
        return path;
    }
}
