package com.greenaddress.greenbits.wallets;

import android.util.Log;
import android.util.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.BlindedScriptsData;
import com.greenaddress.greenapi.data.HWDeviceRequiredData;
import com.greenaddress.greenapi.data.HardwareCodeResolverData;
import com.greenaddress.greenbits.ui.GaActivity;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class HardwareCodeResolver implements CodeResolver {
    private final static String TAG = "HWC";
    private HWWallet hwWallet;
    private GaActivity parent;
    private final Map<Pair<String, String>, String> mNoncesCache = new ConcurrentHashMap<>();

    public HardwareCodeResolver(final GaActivity activity) {
        this.parent = activity;
        this.hwWallet = activity.getSession().getHWWallet();
    }

    public HardwareCodeResolver(final GaActivity activity, final HWWallet hwWallet) {
        this.parent = activity;
        this.hwWallet = hwWallet;
    }

    @Override
    public synchronized SettableFuture<String> hardwareRequest(final HWDeviceRequiredData requiredData) {
        final SettableFuture<String> future = SettableFuture.create();
        final HardwareCodeResolverData data = new HardwareCodeResolverData();

        final List<String> nonces = new ArrayList<>();

        switch (requiredData.getAction()) {

        case "get_xpubs":
            final List<String> xpubs = hwWallet.getXpubs(parent, requiredData.getPaths());
            data.setXpubs(xpubs);
            break;

        case "sign_message":
            try {
                final String derHex = hwWallet.signMessage(parent, requiredData.getPath(), requiredData.getMessage());
                data.setSignature(derHex);
            } catch (final Exception e) {
                future.set(null);
                return future;
            }
            break;

        case "sign_tx":
            final List<String> derHexSigs;
            if (hwWallet.getNetwork().getLiquid()) {
                HWWallet.LiquidHWResult result  = hwWallet.signLiquidTransaction(parent, requiredData.getTransaction(),
                                                                                 requiredData.getSigningInputs(),
                                                                                 requiredData.getTransactionOutputs(),
                                                                                 requiredData.getSigningTransactions(),
                                                                                 requiredData.getSigningAddressTypes());

                derHexSigs = result.getSignatures();

                data.setAssetCommitments(result.getAssetCommitments());
                data.setValueCommitments(result.getValueCommitments());
                data.setAssetblinders(result.getAssetBlinders());
                data.setAmountblinders(result.getAmountBlinders());
            } else {
                derHexSigs = hwWallet.signTransaction(parent, requiredData.getTransaction(),
                                                      requiredData.getSigningInputs(),
                                                      requiredData.getTransactionOutputs(),
                                                      requiredData.getSigningTransactions(),
                                                      requiredData.getSigningAddressTypes());
            }
            data.setSignatures(derHexSigs);
            break;

        case "get_receive_address":
            final String script = requiredData.getAddress().get("blinding_script_hash");
            final String blindingKey = hwWallet.getBlindingKey(parent, script);
            data.setBlindingKey(blindingKey);
            break;

        case "get_balance":
        case "get_transactions":
        case "get_unspent_outputs":
        case "get_subaccounts":
        case "get_subaccount":
        case "get_expired_deposits":
            for (BlindedScriptsData elem : requiredData.getBlindedScripts()) {
                if (!mNoncesCache.containsKey(Pair.create(elem.getPubkey(), elem.getScript()))) {
                    final String nonce = hwWallet.getBlindingNonce(parent, elem.getPubkey(), elem.getScript());
                    mNoncesCache.put(Pair.create(elem.getPubkey(), elem.getScript()), nonce);
                    nonces.add(nonce);
                } else {
                    nonces.add(mNoncesCache.get(Pair.create(elem.getPubkey(), elem.getScript())));
                }
            }

            data.setNonces(nonces);
            break;

        case "create_transaction":
            final Map<String, String> blindingKeys = new HashMap<>();

            if (requiredData.getTransaction() != null && requiredData.getTransaction().get("change_address") != null) {
                for (Iterator<Map.Entry<String, JsonNode>> it =
                         requiredData.getTransaction().get("change_address").fields();
                     it.hasNext(); ) {
                    Map.Entry<String, JsonNode> changeAddr = it.next();

                    final String key = hwWallet.getBlindingKey(parent, changeAddr.getValue().get(
                                                                   "blinding_script_hash").asText());
                    blindingKeys.put(changeAddr.getKey(), key);
                }
            }

            data.setBlindingKeys(blindingKeys);
            break;

        default:
            Log.w(TAG, "unknown action " + requiredData.getAction());
            future.set(null);
            return future;
        }
        future.set(data.toString());
        return future;
    }

    @Override
    public void dismiss() { }

    @Override
    public SettableFuture<String> code(String method, final Integer attemptsRemaining) {
        return null;
    }
}
