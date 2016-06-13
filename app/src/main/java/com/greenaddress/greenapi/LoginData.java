package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class LoginData {
    private final static int EPOCH_START = 1393628400;

    public final String exchange;
    public final String currency;
    public final Map<String, Object> userConfig;
    public Map<String, Object> feeEstimates;
    public final ArrayList subaccounts;
    public final String receivingId;
    public int[] gaUserPath;  // can change on first login (registration)
    public final int earliest_key_creation_time;
    public final boolean segwit;
    public final boolean rbf;

    public LoginData(final Map<?, ?> map) throws IOException {
        this.exchange = (String) map.get("exchange");
        this.currency = (String) map.get("currency");
        // The name 'appearance' for user config is historical
        final String cfg = (String) map.get("appearance");
        this.userConfig = new MappingJsonFactory().getCodec().readValue(cfg, Map.class);
        this.subaccounts = (ArrayList) map.get("subaccounts");
        setGaUserPath(Wally.hex_to_bytes((String) map.get("gait_path")));
        this.receivingId = (String) map.get("receiving_id");
        if (map.get("segwit") == null) {
            this.segwit = false;
        } else {
            this.segwit = (Boolean) map.get("segwit");
        }
        if (map.get("rbf") == null) {
            this.rbf = false;
            this.feeEstimates = null;
        } else {
            this.rbf = (Boolean) map.get("rbf");
            if (this.rbf) {
                this.feeEstimates = (Map<String, Object>) map.get("fee_estimates");
            } else {
                this.feeEstimates = null;
            }
        }
        if (map.containsKey("earliest_key_creation_time")) {
            this.earliest_key_creation_time = (Integer) map.get("earliest_key_creation_time");
        } else {
            // server doesn't provide it yet, set it to EPOCH
            this.earliest_key_creation_time = EPOCH_START;
        }
    }

    private int u8(int i) { return i < 0 ? 256 + i : i; }

    public void setGaUserPath(final byte[] path) {
        gaUserPath = new int[32];
        for (int i = 0; i < 32; ++i)
            gaUserPath[i] = u8(path[i * 2]) * 256 + u8(path[i * 2 + 1]);
    }
}
