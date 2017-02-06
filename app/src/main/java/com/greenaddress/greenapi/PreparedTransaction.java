package com.greenaddress.greenapi;

import android.webkit.URLUtil;

import com.blockstream.libwally.Wally;
import com.greenaddress.greenbits.GaService;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.bitcoinj.core.NetworkParameters;
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

    public final Integer mChangePointer;
    public final int mSubAccount;
    public final Boolean mRequiresTwoFactor;
    public final List<Output> mPrevOutputs = new ArrayList<>();
    public final Transaction mDecoded;
    public final Map<String, Transaction> mPrevoutRawTxs = new HashMap<>();
    public final byte[] mTwoOfThreeBackupChaincode;
    public final byte[] mTwoOfThreeBackupPubkey;

    private static byte[] getBytes(final Map<String, ?> map, final String key) {
        return map == null ? null : Wally.hex_to_bytes((String) map.get(key));
    }

    public PreparedTransaction(final Integer changePointer, final int subAccount, final Transaction decoded, final Map<String, ?> twoOfThree) {
        mChangePointer = changePointer;
        mSubAccount = subAccount;
        mRequiresTwoFactor = false;
        mDecoded = decoded;
        mTwoOfThreeBackupChaincode = getBytes(twoOfThree, "2of3_backup_chaincode");
        mTwoOfThreeBackupPubkey = getBytes(twoOfThree, "2of3_backup_pubkey");
    }

    public static class PreparedData {

        public PreparedData(final Map<?, ?> values,
                            final Map<String, ?> privateData,
                            final ArrayList<Map<String, ?>> subAccounts,
                            final OkHttpClient client)

        {
            mValues = values;
            mPrivateData = privateData;
            mSubAccounts = subAccounts;
            mClient = client;

        }
        final Map<?, ?> mValues;
        final Map<String, ?> mPrivateData;
        final ArrayList<Map<String, ?>> mSubAccounts;
        final OkHttpClient mClient;
    }

    public PreparedTransaction(final PreparedData pte) {

        if (pte.mPrivateData == null || pte.mPrivateData.get("subaccount") == null) {
            mSubAccount = 0;
            mTwoOfThreeBackupChaincode = null;
            mTwoOfThreeBackupPubkey = null;
        } else {
            mSubAccount = (Integer) pte.mPrivateData.get("subaccount");
            byte[] chaincode = null, pubkey = null;
            if (mSubAccount != 0) {
                // Check if the sub-account is 2of3 and if so store its chaincode/public key
                for (final Map<String, ?> m : pte.mSubAccounts)
                    if (m.get("type").equals("2of3") && m.get("pointer").equals(mSubAccount)) {
                        chaincode = getBytes(m, "2of3_backup_chaincode");
                        pubkey = getBytes(m, "2of3_backup_pubkey");
                        break;
                    }
            }
            mTwoOfThreeBackupChaincode = chaincode;
            mTwoOfThreeBackupPubkey = pubkey;
        }

        for (final Object obj : (List) pte.mValues.get("prev_outputs"))
            mPrevOutputs.add(new Output((Map<?, ?>) obj));

        if (pte.mValues.get("change_pointer") != null)
            mChangePointer = Integer.parseInt(pte.mValues.get("change_pointer").toString());
        else
            mChangePointer = null;

        mRequiresTwoFactor = (Boolean) pte.mValues.get("requires_2factor");
        mDecoded = GaService.buildTransaction((String) pte.mValues.get("tx"));

        if (Network.NETWORK.equals(NetworkParameters.fromID(NetworkParameters.ID_REGTEST))) {
            // For REGTEST we fetch the previous outputs inline
            // FIXME: Do this for the other environments too after more testing
            final Map<String, String> txs;
            txs = (Map<String, String>) pte.mValues.get("prevout_rawtxs");
            // if txs is null, the caller passed 'skip' to avoid returning previous txs
            if (txs != null)
                for (final String txHash : txs.keySet())
                    mPrevoutRawTxs.put(txHash, GaService.buildTransaction(txs.get(txHash)));
            return;
        }

        // Return early if no rawtxs url is given, assumes user asked for 'skip'
        try {
            if (!URLUtil.isValidUrl((String) pte.mValues.get("prevout_rawtxs")))
                return;
        } catch (final Exception e) {
            return;
        }

        final Request request = new Request.Builder()
                .url((String)pte.mValues.get("prevout_rawtxs"))
                .build();
        try {
            final String jsonStr = pte.mClient.newCall(request).execute().body().string();

            final JSONObject prevout_rawtxs = new JSONObject(jsonStr);
            final Iterator<?> keys = prevout_rawtxs.keys();

            while (keys.hasNext()) {
                final String k = (String)keys.next();
                mPrevoutRawTxs.put(k, GaService.buildTransaction(prevout_rawtxs.getString(k)));
            }

        } catch (final IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
