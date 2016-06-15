package com.greenaddress.greenapi;

import android.webkit.URLUtil;

import com.blockstream.libwally.Wally;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.bitcoinj.core.Transaction;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PreparedTransaction {

    public final Integer change_pointer;
    public final int subAccount;
    public final Boolean requires_2factor;
    public final List<Output> prev_outputs = new ArrayList<>();
    public final Transaction decoded;
    public final Map<String, Transaction> prevoutRawTxs;
    public final byte[] twoOfThreeBackupChaincode;
    public final byte[] twoOfThreeBackupPubkey;

    private static byte[] getBytes(final Map<String, ?> map, final String key) {
        return map == null ? null : Wally.hex_to_bytes((String) map.get(key));
    }

    public PreparedTransaction(final Integer change_pointer, final int subAccount, final Transaction decoded, final Map<String, ?> twoOfThree) {
        this.change_pointer = change_pointer;
        this.subAccount = subAccount;
        this.requires_2factor = false;
        this.decoded = decoded;
        this.prevoutRawTxs = new HashMap<>();
        this.twoOfThreeBackupChaincode = getBytes(twoOfThree, "2of3_backup_chaincode");
        this.twoOfThreeBackupPubkey = getBytes(twoOfThree, "2of3_backup_pubkey");
    }

    public static class PreparedData {

        public PreparedData(final Map<?, ?> values,
                            final Map<String, ?> privateData,
                            final ArrayList subAccounts,
                            final OkHttpClient client)

        {
            this.values = values;
            this.privateData = privateData;
            this.subAccounts = subAccounts;
            this.client = client;

        }
        final Map<?, ?> values;
        final Map<String, ?> privateData;
        final ArrayList subAccounts;
        final OkHttpClient client;

    }

    public PreparedTransaction(final PreparedData pte) {

        this.prevoutRawTxs = new HashMap<>();

        if (pte.privateData == null || pte.privateData.get("subaccount") == null) {
            this.subAccount = 0;
            this.twoOfThreeBackupChaincode = null;
            this.twoOfThreeBackupPubkey = null;
        } else {
            this.subAccount = (Integer) pte.privateData.get("subaccount");
            byte[] chaincode = null, pubkey = null;
            if (this.subAccount != 0) {
                // Check if the sub-account is 2of3 and if so store its chaincode/public key
                for (final Object s : pte.subAccounts) {
                    final Map<String, ?> m = (Map) s;
                    if (m.get("type").equals("2of3") && m.get("pointer").equals(this.subAccount)) {
                        chaincode = getBytes(m, "2of3_backup_chaincode");
                        pubkey = getBytes(m, "2of3_backup_pubkey");
                        break;
                    }
                }
            }
            this.twoOfThreeBackupChaincode = chaincode;
            this.twoOfThreeBackupPubkey = pubkey;
        }

        for (final Object obj : (List) pte.values.get("prev_outputs"))
            this.prev_outputs.add(new Output((Map<?, ?>) obj));

        if (pte.values.get("change_pointer") != null)
            this.change_pointer = Integer.parseInt(pte.values.get("change_pointer").toString());
        else
            this.change_pointer = null;

        this.requires_2factor = (Boolean) pte.values.get("requires_2factor");
        this.decoded = new Transaction(Network.NETWORK, Wally.hex_to_bytes(pte.values.get("tx").toString()));

        // return early if no rawtxs url is given, assumes user asked for 'skip'
        try {
            if (!URLUtil.isValidUrl((String) pte.values.get("prevout_rawtxs"))) {
                return;
            }
        } catch (final Exception e) {
            return;
        }


        final Request request = new Request.Builder()
                .url((String)pte.values.get("prevout_rawtxs"))
                .build();
        try {
            final String jsonStr = pte.client.newCall(request).execute().body().string();

            final JSONObject prevout_rawtxs = new JSONObject(jsonStr);
            final Iterator<?> keys = prevout_rawtxs.keys();

            while (keys.hasNext()) {
                final String k = (String)keys.next();
                prevoutRawTxs.put(k, new Transaction(Network.NETWORK, Wally.hex_to_bytes(prevout_rawtxs.getString(k))));
            }

        } catch (final IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
