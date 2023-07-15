package com.greenaddress.greenbits.wallets;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blockstream.HardwareQATester;
import com.blockstream.HwWalletLogin;
import com.blockstream.common.extensions.GdkExtensionsKt;
import com.blockstream.common.gdk.Gdk;
import com.blockstream.common.gdk.data.Account;
import com.blockstream.common.gdk.data.Device;
import com.blockstream.common.gdk.data.InputOutput;
import com.blockstream.common.gdk.data.Network;
import com.blockstream.common.gdk.device.BlindingFactorsResult;
import com.blockstream.common.gdk.device.HardwareWalletInteraction;
import com.blockstream.common.gdk.device.SignTransactionResult;
import com.blockstream.jade.JadeAPI;
import com.blockstream.jade.data.JadeNetworks;
import com.blockstream.jade.data.JadeState;
import com.blockstream.jade.data.VersionInfo;
import com.blockstream.jade.entities.Commitment;
import com.blockstream.jade.entities.JadeError;
import com.blockstream.jade.entities.JadeVersion;
import com.blockstream.jade.entities.SignMessageResult;
import com.blockstream.jade.entities.SignTxInputsResult;
import com.blockstream.jade.entities.TxChangeOutput;
import com.blockstream.jade.entities.TxInput;
import com.blockstream.jade.entities.TxInputBtc;
import com.blockstream.jade.entities.TxInputLiquid;
import com.blockstream.libwally.Wally;
import com.google.common.io.BaseEncoding;
import com.greenaddress.greenapi.HWWallet;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import kotlinx.coroutines.CompletableDeferred;
import kotlinx.coroutines.CompletableDeferredKt;
import kotlinx.coroutines.flow.StateFlow;

abstract public class JadeHWWalletJava extends HWWallet {
    private static final String TAG = "JadeHWWallet";

    // FIXME: Should be "0.1.48" once that is released
    private static final JadeVersion JADE_VER_SUPPORTS_SWAPS = new JadeVersion("0.1.48-alpha1");
    private final JadeAPI jade;

    // FIXME: remove (an assume true) when 0.1.48 is made minimum allowed version.
    private final boolean has_swap_support;

    public JadeHWWalletJava(final Gdk gdk, final JadeAPI jade, final Device device, final VersionInfo verInfo, final HardwareQATester hardwareQATester) {
        this.mGdk = gdk;
        super.mDevice = device;
        this.jade = jade;
        this.mHardwareQATester = hardwareQATester;
        this.mFirmwareVersion = verInfo.getJadeVersion();
        this.mModel = verInfo.getBoardType();

        // Cache whether this fw version supports swap signing (and more efficient blinding calls)
        final boolean pre_swap_support = new JadeVersion(this.mFirmwareVersion).isLessThan(JADE_VER_SUPPORTS_SWAPS);
        this.has_swap_support = !pre_swap_support;
    }

    @Override
    public synchronized void disconnect() {
        this.jade.disconnect();
    }

    // Helper to map a [single-sig] address type into a jade descriptor variant string
    private static String mapAddressType(final String addrType) {
        switch (addrType) {
            case "p2pkh": return "pkh(k)";
            case "p2wpkh": return "wpkh(k)";
            case "p2sh-p2wpkh": return "sh(wpkh(k))";
            default: return null;
        }
    }

    // Helper to push entropy into jade, and then call 'authUser()' in a loop
    // (until correctly setup and user authenticated etc).
    public boolean authUser(final HwWalletLogin hwLoginBridge) throws Exception {
        // Push some extra entropy into Jade

        jade.addEntropy(mGdk.getRandomBytes(32));

        VersionInfo info = jade.getVersionInfo();
        JadeState state = info.getJadeState();
        JadeNetworks networks = info.getJadeNetworks();

        String network;
        if(state == JadeState.TEMP || state == JadeState.UNSAVED || state == JadeState.UNINIT || networks == JadeNetworks.ALL){
            // Ask network from user
            Network requestNetwork = hwLoginBridge.requestNetwork();
            if(requestNetwork == null){
                throw new Exception("id_action_canceled");
            }
            network = requestNetwork.getCanonicalNetworkId();
        }else{
            if(networks == JadeNetworks.MAIN){
                network = "mainnet";
            }else{
                network = "testnet";
            }
        }

        // JADE_STATE => READY  (device unlocked / ready to use)
        // anything else ( LOCKED | UNSAVED | UNINIT | TEMP) will need an authUser first to unlock
        if (state != JadeState.READY) {
            CompletableDeferred completable = CompletableDeferredKt.CompletableDeferred(null);

            // JADE_STATE => TEMP no need for PIN entry
            if (hwLoginBridge != null && state != JadeState.TEMP) {
                if(state != JadeState.UNINIT){
                    hwLoginBridge.interactionRequest(this, completable, "id_enter_pin_on_jade");
                }
            }

            try {
                // Authenticate with pinserver (loop/retry on failure)
                // Note: this should be a no-op if the user is already authenticated on the device.
                while (!this.jade.authUser(network)) {
                    Log.w(TAG, "Jade authentication failed");
                }
                completable.complete(true);
            }catch (Exception e){
                completable.completeExceptionally(e);
                throw e;
            }
        }

        return true;
    }

//    // Authenticate Jade with pinserver and check firmware version with fw-server
//    public Single<JadeHWWalletJava> authenticate(final HwLoginBridge hwLoginBridge, final JadeFirmwareManager jadeFirmwareManager) throws Exception {
//        /*
//         * 1. check firmware (and maybe OTA) any completely uninitialised device (ie no keys/pin set - no unlocking needed)
//         * 2. authenticate the user (see above)
//         * 3. check the firmware again (and maybe OTA) for devices that are set-up (and hence needed unlocking first)
//         * 4. authenticate the user *if required* - as we may have OTA'd and rebooted the hww.  Should be a no-op if not needed.
//         */
//        return Single.just(this)
//                .map(hww -> jadeFirmwareManager.checkFirmwareBlocking(jade, false))
//                .map(fwValid -> authUser(hwLoginBridge))
//                .map(authed -> jadeFirmwareManager.checkFirmwareBlocking(jade, true))
//                .flatMap(fwValid -> {
//                    if (fwValid) {
//                        authUser(hwLoginBridge);  // re-auth if required
//                        return Single.just(this);
//                    } else {
//                        return Single.error(new JadeError(JadeError.UNSUPPORTED_FIRMWARE_VERSION,
//                                "Insufficient/invalid firmware version", null));
//                    }
//                });
//    }

    // Helper to map an optional hex string into an array of bytes - null input is returned as null
    private static byte[] hexToBytes(final String hexstr) {
        return hexstr != null ? Wally.hex_to_bytes(hexstr) : null;
    }

    // Helper to map an optional array of bytes into a hex string - null input is returned as null
    private static String hexFromBytes(final byte[] bytes) {
        return bytes != null ? Wally.hex_from_bytes(bytes) : null;
    }

    // Helper to map an array of n byte arrays into an array of n hex strings - null input is returned as null
    private static List<String> hexFromBytes(final List<byte[]> lstByteArrays) {
        if (lstByteArrays == null) {
            return null;
        }

        final List<String> lstHexes = new ArrayList<>(lstByteArrays.size());
        for (final byte[] bytes : lstByteArrays) {
            lstHexes.add(hexFromBytes(bytes));
        }
        return lstHexes;
    }

    // Helper to turn the BIP32 paths back into a list of Longs, rather than a list of Integers
    // (which may well be expressed as negative [for hardened paths]).
    private static List<Long> getUnsignedPath(final List<Integer> signed) {
        final List<Long> unsigned = new ArrayList<>(signed.size());
        for (final Integer i : signed) {
            if (i < 0) {
                final long large = 1L + Integer.MAX_VALUE + (i - Integer.MIN_VALUE);
                unsigned.add(large);
            } else {
                unsigned.add(i.longValue());
            }
        }
        return unsigned;
    }

    @NonNull
    @Override
    public synchronized List<String> getXpubs(@NonNull Network network, @Nullable HardwareWalletInteraction hwInteraction, @NonNull List<? extends List<Integer>> paths) {
        Log.d(TAG, "getXpubs() for " + paths.size() + " paths (" + network.getId() + ").");

        final String canonicalNetworkId = network.getCanonicalNetworkId();
        try {
            // paths.stream.map(jade::get_xpub).collect(Collectors.toList());
            final List<String> xpubs = new ArrayList<>(paths.size());
            for (final List<Integer> path : paths) {
                final List<Long> unsignedPath = getUnsignedPath(path);
                final String xpub = this.jade.getXpub(canonicalNetworkId, unsignedPath);
                Log.d(TAG, "Got xpub for " + path + ": " + xpub);
                xpubs.add(xpub);
            }

            Log.d(TAG, "getXpubs() returning " + xpubs.size() + " xpubs");
            return xpubs;
        } catch (final Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @NonNull
    @Override
    public synchronized com.blockstream.common.gdk.device.SignMessageResult signMessage(@Nullable HardwareWalletInteraction hwInteraction, @NonNull List<Integer> path, @NonNull String message, boolean useAeProtocol, @Nullable String aeHostCommitment, @Nullable String aeHostEntropy) {
        Log.d(TAG, "signMessage() for message of length " + message.length() + " using path " + path);

        CompletableDeferred completable = CompletableDeferredKt.CompletableDeferred(null);

        try {
            final List<Long> unsignedPath = getUnsignedPath(path);

            if (hwInteraction != null) {
                hwInteraction.interactionRequest(this, completable, "id_check_your_device");
            }

            final SignMessageResult result = this.jade.signMessage(unsignedPath, message, useAeProtocol,
                                                                   hexToBytes(aeHostCommitment),
                                                                   hexToBytes(aeHostEntropy));

            // Convert the signature from Base64 into into DER hex for GDK
            byte[] sigDecoded = BaseEncoding.base64().decode(result.getSignature());

            // Need to truncate lead byte if recoverable signature
            if (sigDecoded.length == Wally.EC_SIGNATURE_RECOVERABLE_LEN) {
                sigDecoded = Arrays.copyOfRange(sigDecoded, 1, sigDecoded.length);
            }

            final byte[] sigDer = new byte[Wally.EC_SIGNATURE_DER_MAX_LEN];
            final int len = Wally.ec_sig_to_der(sigDecoded, sigDer);
            final String sigDerHex =  hexFromBytes(Arrays.copyOfRange(sigDer, 0, len));

            Log.d(TAG, "signMessage() returning: " + sigDerHex);
            return new com.blockstream.common.gdk.device.SignMessageResult(sigDerHex, hexFromBytes(result.getSignerCommitment()));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }finally {
            completable.complete(true);
        }
    }

    // Helper to get the change paths for auto-validation
    private static List<TxChangeOutput> getChangeData(final List<InputOutput> outputs) {
        // Get the change outputs and paths
        final List<TxChangeOutput> change = new ArrayList<>(outputs.size());
        for (final InputOutput output : outputs) {
            if (output.isChange() != null && output.isChange()) {
                // change - get path
                change.add(new TxChangeOutput(output.getUserPath(),
                                              output.getRecoveryXpub(),
                                              "csv".equals(output.getAddressType()) ? output.getSubtype() : 0,
                                              mapAddressType(output.getAddressType())));
            } else {
                // Not change, put null place holder
                change.add(null);
            }
        }
        return change;
    }

    @NonNull
    @Override
    public synchronized SignTransactionResult signTransaction(@NonNull Network network, @Nullable HardwareWalletInteraction hwInteraction, @NonNull String transaction, @NonNull List<InputOutput> inputs, @NonNull List<InputOutput> outputs, @Nullable Map<String, String> transactions, boolean useAeProtocol) {
        Log.d(TAG, "signTransaction() called for " + inputs.size() + " inputs");

        final byte[] txBytes = hexToBytes(transaction);

        if(network.isLiquid()){
            try {

                // Load the tx into wally for legacy fw versions as will need it later
                // to access the output's asset[generator] and value[commitment].
                // NOTE: 0.1.48+ Jade fw does need these extra values passed explicitly so
                // no need to parse/load the transaction into wally.
                // FIXME: remove when 0.1.48 is made minimum allowed version.
                final Object wallytx = !this.has_swap_support ? Wally.tx_from_bytes(txBytes, Wally.WALLY_TX_FLAG_USE_ELEMENTS) : null;

                // Collect data from the tx inputs
                final List<TxInputLiquid> txInputs = new ArrayList<>(inputs.size());
                for (final InputOutput input : inputs) {
                    // Get the input in the form Jade expects
                    final byte[] script = hexToBytes(input.getPrevoutScript());
                    final byte[] commitment = hexToBytes(input.getCommitment());
                    txInputs.add(new TxInputLiquid(input.isSegwit(),
                            script,
                            commitment,
                            input.getUserPath(),
                            hexToBytes(input.getAeHostCommitment()),
                            hexToBytes(input.getAeHostEntropy())));
                }

                // Get blinding factors and unblinding data per output - null for unblinded outputs
                // Assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
                final List<Commitment> trustedCommitments = new ArrayList<>(outputs.size());
                for (int i = 0; i < outputs.size(); ++i) {
                    final InputOutput output = outputs.get(i);
                    if (output.getBlindingKey() != null) {
                        final Commitment commitment = new Commitment();
                        commitment.setAssetId(output.getAssetIdBytes());
                        commitment.setValue(output.getSatoshi());
                        commitment.setAbf(output.getAbfs());
                        commitment.setVbf(output.getVbfs());
                        commitment.setBlindingKey(output.getPublicKeyBytes());

                        // Add asset-generator and value-commitment for legacy fw versions
                        // NOTE: 0.1.48+ Jade fw does need these extra values passed explicitly
                        if (wallytx != null) {
                            commitment.setAssetGenerator(Wally.tx_get_output_asset(wallytx, i));
                            commitment.setValueCommitment(Wally.tx_get_output_value(wallytx, i));
                        }

                        trustedCommitments.add(commitment);
                    } else {
                        // Add a 'null' commitment for unblinded output
                        trustedCommitments.add(null);
                    }
                }

                // Get the change outputs and paths
                final List<TxChangeOutput> change = getChangeData(outputs);

                // Make jade-api call to sign the txn
                final String canonicalNetworkId = network.getCanonicalNetworkId();
                final SignTxInputsResult result = this.jade.signLiquidTx(canonicalNetworkId, useAeProtocol, txBytes,
                        txInputs, trustedCommitments, change);
                // Pivot data into return structure
                Log.d(TAG, "signLiquidTransaction() returning " + result.getSignatures().size() + " signatures");
                return new SignTransactionResult(hexFromBytes(result.getSignatures()), hexFromBytes(result.getSignerCommitments()));
            } catch (final Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }else {

            try {
                if (transactions == null || transactions.isEmpty()) {
                    throw new RuntimeException("Input transactions missing");
                }

                // Get the inputs in the form Jade expects
                final List<TxInput> txInputs = new ArrayList<>(inputs.size());
                for (final InputOutput input : inputs) {
                    final boolean swInput = input.isSegwit();
                    final byte[] script = hexToBytes(input.getPrevoutScript());

                    if (swInput && inputs.size() == 1) {
                        // Single SegWit input - can skip sending entire tx and just send the sats amount
                        txInputs.add(new TxInputBtc(swInput,
                                null,
                                script,
                                input.getSatoshi(),
                                input.getUserPath(),
                                hexToBytes(input.getAeHostCommitment()),
                                hexToBytes(input.getAeHostEntropy())));
                    } else {
                        // Non-SegWit input or there are several inputs - in which case we always send
                        // the entire prior transaction up to Jade (so it can verify the spend amounts).
                        final String txhex = transactions.get(input.getTxHash());
                        if (txhex == null) {
                            throw new RuntimeException("Required input transaction not found: " + input.getTxHash());
                        }
                        final byte[] inputTx = hexToBytes(txhex);
                        txInputs.add(new TxInputBtc(swInput,
                                inputTx,
                                script,
                                null,
                                input.getUserPath(),
                                hexToBytes(input.getAeHostCommitment()),
                                hexToBytes(input.getAeHostEntropy())));
                    }
                }

                // Get the change outputs and paths
                final List<TxChangeOutput> change = getChangeData(outputs);

                // Make jade-api call
                final String canonicalNetworkId = network.getCanonicalNetworkId();
                final SignTxInputsResult result = this.jade.signTx(canonicalNetworkId, useAeProtocol, txBytes, txInputs, change);

                Log.d(TAG, "signTransaction() returning " + result.getSignatures().size() + " signatures");
                return new SignTransactionResult(hexFromBytes(result.getSignatures()), hexFromBytes(result.getSignerCommitments()));
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @NonNull
    @Override
    public synchronized String getMasterBlindingKey(@Nullable HardwareWalletInteraction hwInteraction) {
        Log.d(TAG, "getMasterBlindingKey() called");

        CompletableDeferred completable = CompletableDeferredKt.CompletableDeferred(null);
        try {
            if(hwInteraction != null) {
                hwInteraction.interactionRequest(this, completable, "id_check_your_device");
            }
            final byte[] masterkey = this.jade.getMasterBlindingKey();
            final String keyHex = hexFromBytes(masterkey);
            Log.d(TAG, "getMasterBlindingKey() returning " + keyHex);
            return keyHex;
        } catch (final JadeError e) {
            if (e.getCode() == JadeError.CBOR_RPC_USER_CANCELLED) {
                // User cancelled on the device - return as empty string (rather than error)
                return "";
            }
            throw new RuntimeException(e.getMessage());
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        finally {
            completable.complete(true);
        }
    }

    @NonNull
    @Override
    public synchronized String getBlindingKey(@Nullable HardwareWalletInteraction hwInteraction, @NonNull String scriptHex) {
        Log.d(TAG, "getBlindingKey() for script of length " + scriptHex.length());

        try {
            final byte[] script = hexToBytes(scriptHex);
            final byte[] bkey = this.jade.getBlindingKey(script);
            final String keyHex = hexFromBytes(bkey);
            Log.d(TAG, "getBlindingKey() returning " + keyHex);
            return keyHex;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @NonNull
    @Override
    public synchronized String getBlindingNonce(@Nullable HardwareWalletInteraction hwInteraction, @NonNull String pubkey, @NonNull String scriptHex) {
        Log.d(TAG, "getBlindingNonce() for script of length " + scriptHex.length() + " and pubkey " + pubkey);

        try {
            final byte[] script = hexToBytes(scriptHex);
            final byte[] pkey = hexToBytes(pubkey);
            final byte[] nonce = this.jade.getSharedNonce(script, pkey);
            final String nonceHex = hexFromBytes(nonce);
            Log.d(TAG, "getBlindingNonce() returning " + nonceHex);
            return nonceHex;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static byte[] sliceReversed(final byte[] data, final int offset, final int len) {
        final byte[] result = new byte[len];
        for (int i = 0; i < len; ++i) {
            result[i] = data[offset + len - i - 1];
        }
        return result;
    }

    @NonNull
    @Override
    public synchronized  BlindingFactorsResult getBlindingFactors(@Nullable HardwareWalletInteraction hwInteraction, @Nullable List<InputOutput> inputs, @Nullable List<InputOutput> outputs) {
        Log.d(TAG, "getBlindingFactors() called for " + outputs.size() + " outputs");

        try {
            // Compute hashPrevouts to derive deterministic blinding factors from
            final ByteArrayOutputStream txhashes = new ByteArrayOutputStream(inputs.size() * Wally.WALLY_TXHASH_LEN);
            final int[] outputIdxs = new int[inputs.size()];
            for (int i = 0; i < inputs.size(); ++i) {
                final InputOutput input = inputs.get(i);
                txhashes.write(input.getTxid());
                outputIdxs[i] = input.getPtIdxInt();
            }
            // FIXME: Remove final (unnecessary) 'null' parameter when wally swig wrapper fixed
            final byte[] hashPrevouts = Wally.get_hash_prevouts(txhashes.toByteArray(), outputIdxs, null);

            // Enumerate the outputs and provide blinding factors as needed
            // Assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
            final BlindingFactorsResult rslt = new BlindingFactorsResult(outputs.size());
            for (int i = 0; i < outputs.size(); ++i) {
                final InputOutput output = outputs.get(i);
                if (output.getBlindingKey() != null) {
                    // Call Jade to get the blinding factors
                    // NOTE: 0.1.48+ Jade fw accepts 'ASSET_AND_VALUE', and returns abf and vbf concatenated abf||vbf
                    // (Previous versions need two calls, for 'ASSET' and 'VALUE' separately)
                    // FIXME: remove when 0.1.48 is made minimum allowed version.
                    if (this.has_swap_support) {
                        final byte[] bfs = this.jade.getBlindingFactor(hashPrevouts, i, "ASSET_AND_VALUE");
                        rslt.append(hexFromBytes(sliceReversed(bfs, 0, Wally.BLINDING_FACTOR_LEN)),
                                hexFromBytes(sliceReversed(bfs, Wally.BLINDING_FACTOR_LEN, Wally.BLINDING_FACTOR_LEN)));
                    } else {
                        final byte[] abf = this.jade.getBlindingFactor(hashPrevouts, i, "ASSET");
                        final byte[] vbf = this.jade.getBlindingFactor(hashPrevouts, i, "VALUE");
                        rslt.append(hexFromBytes(GdkExtensionsKt.reverseBytes(abf)), hexFromBytes(GdkExtensionsKt.reverseBytes(vbf)));
                    }
                } else {
                    // Empty string placeholders
                    rslt.append("", "");
                }
                Log.d(TAG, "getBlindingFactors() for output " + i + ": " + rslt.getAssetblinders().get(i) + " / " + rslt.getAmountblinders().get(i));
            }
            Log.d(TAG, "getBlindingFactors() returning for " + outputs.size() + " outputs");
            return rslt;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    @Override
    public synchronized String getGreenAddress(Network network, @Nullable HardwareWalletInteraction hwInteraction, Account account, List<Long> path, long csvBlocks) throws Exception {
        try {
            final String canonicalNetworkId = network.getCanonicalNetworkId();
            if (network.isMultisig()) {
                // Green Multisig Shield - pathlen should be 2 for subact 0, and 4 for subact > 0
                // In any case the last two entries are 'branch' and 'pointer'
                final int pathlen = path.size();
                final long branch = path.get(pathlen - 2);
                final long pointer = path.get(pathlen - 1);
                String recoveryxpub = null;

                Log.d(TAG,"getGreenAddress() (multisig shield) for subaccount: " + account.getPointer() + ", branch: "
                        + branch + ", pointer " + pointer);

                // Jade expects any 'recoveryxpub' to be at the subact/branch level, consistent with tx outputs - but gdk
                // subaccount data has the base subaccount chain code and pubkey - so we apply the branch derivation here.
                if (account.getRecoveryChainCode() != null && account.getRecoveryChainCode().length() > 0) {
                    final Object subactkey = Wally.bip32_pub_key_init(
                            network.getVerPublic(), 0, 0,
                            account.getRecoveryChainCodeAsBytes(), account.getRecoveryPubKeyAsBytes());
                    final Object branchkey = Wally.bip32_key_from_parent(subactkey, branch,
                            Wally.BIP32_FLAG_KEY_PUBLIC |
                                    Wally.BIP32_FLAG_SKIP_HASH);
                    recoveryxpub = Wally.bip32_key_to_base58(branchkey, Wally.BIP32_FLAG_KEY_PUBLIC);
                    Wally.bip32_key_free(branchkey);
                    Wally.bip32_key_free(subactkey);
                }

                // Get receive address from Jade for the path elements given
                final String address = this.jade.getReceiveAddress(canonicalNetworkId,
                        account.getPointer(), branch, pointer,
                        recoveryxpub, csvBlocks);
                Log.d(TAG, "Got green address for branch: " + branch + ", pointer: " + pointer + ": " + address);
                return address;
            } else {
                // Green Electrum Singlesig
                Log.d(TAG,"getGreenAddress() (singlesig) for path: " + path);
                final String variant = mapAddressType(account.getType().getGdkType());
                final String address = this.jade.getReceiveAddress(canonicalNetworkId, variant, path);
                Log.d(TAG, "Got green address for path: " + path + ", type: " + variant + ": " + address);
                return address;
            }
        } catch (final JadeError e) {
            if (e.getCode() == JadeError.CBOR_RPC_USER_CANCELLED) {
                // User cancelled on the device - treat as mismatch (rather than error)
                return "";
            }
            throw new RuntimeException(e.getMessage());
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Nullable
    @Override
    public StateFlow<Boolean> getDisconnectEvent() {
        return jade.getDisconnectEvent();
    }
}
