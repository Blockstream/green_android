package com.greenaddress.greenbits.wallets;

import android.util.Log;
import android.util.Pair;

import com.blockstream.gdk.HardwareWalletResolver;
import com.blockstream.gdk.data.DeviceRequiredData;
import com.fasterxml.jackson.databind.JsonNode;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.HWWalletBridge;
import com.greenaddress.greenapi.Session;
import com.greenaddress.greenapi.data.BlindedScriptsData;
import com.greenaddress.greenapi.data.HWDeviceRequiredData;
import com.greenaddress.greenapi.data.HardwareCodeResolverData;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Single;


public class HardwareCodeResolver implements HardwareWalletResolver {
    private final static String TAG = "HWC";
    private HWWallet hwWallet;
    private HWWalletBridge parent;
    private final Map<Pair<String, String>, String> mNoncesCache = new ConcurrentHashMap<>();

    public HardwareCodeResolver(final HWWalletBridge hwWalletBridge) {
        this.parent = hwWalletBridge;
        this.hwWallet = Session.getSession().getHWWallet();
    }

    public HardwareCodeResolver(final HWWalletBridge hwWalletBridge, final HWWallet hwWallet) {
        this.parent = hwWalletBridge;
        this.hwWallet = hwWallet;
    }

    @NotNull
    @Override
    public Single<String> requestDataFromDeviceV3(@NotNull HWDeviceRequiredData requiredData) {
        return Single.create(emitter -> {
            emitter.onSuccess(requestDataFromHardware(requiredData));
        });
    }

    @NotNull
    @Override
    public Single<String> requestDataFromDevice(@NotNull DeviceRequiredData requiredData) {
        return null;
    }

    public synchronized String requestDataFromHardware(final HWDeviceRequiredData requiredData) {
        final HardwareCodeResolverData data = new HardwareCodeResolverData();

        final List<String> nonces = new ArrayList<>();

        switch (requiredData.getAction()) {

        case "get_xpubs":
            final List<String> xpubs = hwWallet.getXpubs(parent, requiredData.getPaths());
            data.setXpubs(xpubs);
            break;

        case "sign_message":
            try {
                final HWWallet.SignMsgResult result = hwWallet.signMessage(parent,requiredData.getPath(), requiredData.getMessage(),
                        requiredData.getUseAeProtocol(), requiredData.getAeHostCommitment(), requiredData.getAeHostEntropy());
                data.setSignerCommitment(result.getSignerCommitment());
                data.setSignature(result.getSignature());

                // Corrupt the commitments to emulate a corrupted wallet
                if(hwWallet.getHardwareEmulator() != null && hwWallet.getHardwareEmulator().getAntiExfilCorruptionForMessageSign()){
                    // Make it random to allow proceeding to a logged in state
                    if(result.getSignerCommitment() != null) {
                        // Corrupt the commitment
                        data.setSignerCommitment(result.getSignerCommitment().replace("0", "1"));
                    }
                }
            } catch (final Exception e) {
                return null;
            }
            break;

        case "sign_tx":
            final HWWallet.SignTxResult result;
            if (hwWallet.getNetwork().getLiquid()) {
                result  = hwWallet.signLiquidTransaction(parent, requiredData.getTransaction(),
                                                               requiredData.getSigningInputs(),
                                                               requiredData.getTransactionOutputs(),
                                                               requiredData.getSigningTransactions(),
                                                               requiredData.getSigningAddressTypes(),
                                                               requiredData.getUseAeProtocol());
                data.setAssetCommitments(result.getAssetCommitments());
                data.setValueCommitments(result.getValueCommitments());
                data.setAssetblinders(result.getAssetBlinders());
                data.setAmountblinders(result.getAmountBlinders());
            } else {
                result = hwWallet.signTransaction(parent, requiredData.getTransaction(),
                                                          requiredData.getSigningInputs(),
                                                          requiredData.getTransactionOutputs(),
                                                          requiredData.getSigningTransactions(),
                                                          requiredData.getSigningAddressTypes(),
                                                          requiredData.getUseAeProtocol());
            }
            data.setSignatures(result.getSignatures());
            data.setSignerCommitments(result.getSignerCommitments());

            // Corrupt the commitments to emulate a corrupted wallet
            if(hwWallet.getHardwareEmulator() != null && hwWallet.getHardwareEmulator().getAntiExfilCorruptionForTxSign()){
                if(result.getSignatures() != null) {
                    // Corrupt the first commitment
                    final ArrayList<String> corrupted = new ArrayList<>(result.getSignerCommitments());
                    corrupted.set(0, corrupted.get(0).replace("0", "1"));
                    data.setSignerCommitments(corrupted);
                }
            }

            break;

        case "get_master_blinding_key":
            final String masterBlindingKey = hwWallet.getMasterBlindingKey(parent);
            data.setMasterBlindingKey(masterBlindingKey);
            break;

        case "get_blinding_nonces":
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

        case "get_blinding_public_keys":
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
            return null;
        }
        return data.toString();
    }
}
