package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import java.util.ArrayList;
import java.util.Map;

public class LoginData {
    public final String exchange;
    public final String currency;
    public final Map<String, Object> userConfig;
    public final ArrayList<Map<String, ?>> subAccounts;
    public final String receivingId;
    public final int[] gaUserPath;
    public final int earliestKeyCreationTime;
    public final boolean isSegwitServer; // Does the server support segwit?
    public final boolean rbf;
    public final Map<String, ?> rawData;

    public LoginData(final Map<String, ?> map) {
        this.exchange = (String) map.get("exchange");
        this.currency = (String) map.get("currency");
        this.subAccounts = (ArrayList) map.get("subaccounts");
        gaUserPath = getPath((String) map.get("gait_path"));
        this.receivingId = (String) map.get("receiving_id");
        this.isSegwitServer = (Boolean) map.get("segwit_server");
        this.rbf = (Boolean) map.get("rbf");
        this.earliestKeyCreationTime = (Integer) map.get("earliest_key_creation_time");
        this.rawData = map;
        this.userConfig = (Map<String, Object>) map.get("appearance");
    }

    private int u8(int i) { return i < 0 ? 256 + i : i; }

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
