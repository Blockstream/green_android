package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import java.util.ArrayList;
import java.util.Map;

public class LoginData {
    public final ArrayList<Map<String, ?>> mSubAccounts;
    public final Map<String, Object> mUserConfig;
    public final int[] mGaitPath;
    public final Map<String, ?> mRawData;

    public LoginData(final Map<String, ?> map) {
        mSubAccounts = (ArrayList) map.get("subaccounts");
        mUserConfig = (Map<String, Object>) map.get("appearance");
        mGaitPath = getPath((String) map.get("gait_path"));
        mRawData = map;
    }

    public <T> T get(final String name) {
        return (T) mRawData.get(name);
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
