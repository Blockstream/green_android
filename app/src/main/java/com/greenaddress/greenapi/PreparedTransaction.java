package com.greenaddress.greenapi;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.bitcoinj.core.Transaction;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PreparedTransaction {

    public final String change_pointer;
    public final int subaccount_pointer;
    public final Boolean requires_2factor;
    public final String tx;
    public final List<Output> prev_outputs = new ArrayList<>();
    public final Transaction decoded;
    public final Map<String, Transaction> prevoutRawTxs = new HashMap<>();
    public final String twoOfThreeBackupChaincode;
    public final String twoOfThreeBackupPubkey;

    public static class PTData {

        public PTData(final Map<?, ?> values,
                      final Map<String, ?> privateData,
                      final ArrayList subaccounts,
                      final OkHttpClient client)

        {
            this.values = values;
            this.privateData = privateData;
            this.subaccounts = subaccounts;
            this.client = client;

        }
        final Map<?, ?> values;
        final Map<String, ?> privateData;
        final ArrayList subaccounts;
        final OkHttpClient client;

    }

    public PreparedTransaction(final PTData pte) {

        String twoOfThreeBackupChaincode = null, twoOfThreeBackupPubkey = null;
        if (pte.privateData != null && pte.privateData.get("subaccount") != null && !pte.privateData.get("subaccount").equals(0)) {
            for (final Object subaccount : pte.subaccounts) {
                final Map<String, ?> subaccountMap = (Map) subaccount;
                if (subaccountMap.get("type").equals("2of3") && subaccountMap.get("pointer").equals(pte.privateData.get("subaccount"))) {
                    twoOfThreeBackupChaincode = (String) subaccountMap.get("2of3_backup_chaincode");
                    twoOfThreeBackupPubkey = (String) subaccountMap.get("2of3_backup_pubkey");
                    break;
                }
            }
        }

        this.subaccount_pointer = (pte.privateData == null || pte.privateData.get("subaccount") == null) ?
               0 : (Integer) pte.privateData.get("subaccount");

        this.twoOfThreeBackupChaincode = twoOfThreeBackupChaincode;
        this.twoOfThreeBackupPubkey = twoOfThreeBackupPubkey;

        final List tmp = (List) pte.values.get("prev_outputs");

        for (final Object obj : tmp) {
            this.prev_outputs.add(new Output((Map<?, ?>) obj));
        }

        if (pte.values.get("change_pointer") != null) {
            this.change_pointer = pte.values.get("change_pointer").toString();
        } else {
            this.change_pointer = null;
        }

        this.requires_2factor = (Boolean) pte.values.get("requires_2factor");
        this.tx = pte.values.get("tx").toString();
        this.decoded = new Transaction(Network.NETWORK, Hex.decode(this.tx));

        final Request request = new Request.Builder()
                .url((String)pte.values.get("prevout_rawtxs"))
                .build();
        try {
            final String jsonStr = pte.client.newCall(request).execute().body().string();

            final JSONObject prevout_rawtxs = new JSONObject(jsonStr);
            final Iterator<?> keys = prevout_rawtxs.keys();

            while (keys.hasNext()) {
                final String k = (String)keys.next();
                prevoutRawTxs.put(k, new Transaction(Network.NETWORK,
                        Hex.decode(prevout_rawtxs.getString(k))));
            }

        } catch (final IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
