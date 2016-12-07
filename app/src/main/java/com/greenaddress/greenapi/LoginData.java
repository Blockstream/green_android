package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import java.util.ArrayList;
import java.util.Map;

public class LoginData {
    public final String exchange;
    public String currency;
    public final Map<String, Object> userConfig;
    public Map<String, Object> feeEstimates;
    public final ArrayList<Map<String, ?>> subAccounts;
    public final String receivingId;
    public int[] gaUserPath;  // can change on first login (registration)
    public final int earliestKeyCreationTime;
    public final boolean isSegwitServer; // Does the server support segwit?
    public final boolean rbf;
    public final Map<String, ?> rawData;

    public LoginData(final Map<String, ?> map) {
        this.exchange = (String) map.get("exchange");
        this.currency = (String) map.get("currency");
        this.subAccounts = (ArrayList) map.get("subaccounts");
        gaUserPath = null;
        final String path = (String) map.get("gait_path");
        if (path != null)
            setGaUserPath(Wally.hex_to_bytes(path));
        this.receivingId = (String) map.get("receiving_id");
        this.isSegwitServer = (Boolean) map.get("segwit_server");
        this.rbf = (Boolean) map.get("rbf");
        if (this.rbf)
            this.feeEstimates = (Map<String, Object>) map.get("fee_estimates");
        else
            this.feeEstimates = null;
        this.earliestKeyCreationTime = (Integer) map.get("earliest_key_creation_time");
        this.rawData = map;
        this.userConfig = (Map<String, Object>) map.get("appearance");
    }

    private int u8(int i) { return i < 0 ? 256 + i : i; }

    public void setGaUserPath(final byte[] path) {
        gaUserPath = new int[32];
        for (int i = 0; i < 32; ++i)
            gaUserPath[i] = u8(path[i * 2]) * 256 + u8(path[i * 2 + 1]);
    }
}
