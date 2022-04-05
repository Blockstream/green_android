package com.greenaddress.greenbits.wallets;

import android.util.Log;

import androidx.annotation.Nullable;

import com.blockstream.gdk.ExtensionsKt;
import com.blockstream.gdk.data.Device;
import com.blockstream.gdk.data.InputOutput;
import com.blockstream.gdk.data.Network;
import com.blockstream.gdk.data.SubAccount;
import com.blockstream.hardware.R;
import com.blockstream.libgreenaddress.GDK;
import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Longs;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.HWWalletBridge;
import com.greenaddress.greenapi.HardwareQATester;
import com.greenaddress.jade.JadeAPI;
import com.greenaddress.jade.entities.Commitment;
import com.greenaddress.jade.entities.JadeError;
import com.greenaddress.jade.entities.SignMessageResult;
import com.greenaddress.jade.entities.SignTxInputsResult;
import com.greenaddress.jade.entities.TxChangeOutput;
import com.greenaddress.jade.entities.TxInput;
import com.greenaddress.jade.entities.TxInputBtc;
import com.greenaddress.jade.entities.TxInputLiquid;
import com.greenaddress.jade.entities.VersionInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class JadeHWWallet extends HWWallet {
    private static final String TAG = "JadeHWWallet";

    private final JadeAPI jade;

    public JadeHWWallet(final JadeAPI jade, final Device device, final HardwareQATester hardwareQATester) {
        super.mDevice = device;
        this.jade = jade;
        this.mHardwareQATester = hardwareQATester;
    }

    @Override
    public void disconnect() {
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
    private boolean authUser(final Network network, final HWWalletBridge hwWalletBridge) throws IOException {
        // Push some extra entropy into Jade
        jade.addEntropy(GDK.get_random_bytes(32));

        VersionInfo info = jade.getVersionInfo();
        String state = info.getJadeState();

        // JADE_STATE => READY | TEMP (these mean unlocked / ready to use)
        // anything else ( LOCKED | UNSAVED | UNINIT) will need an authUser first to unlock

        if (!"READY".equals(state) && !"TEMP".equals(state)) {

            CompletableSubject completable = CompletableSubject.create();

            if (hwWalletBridge != null) {
                hwWalletBridge.interactionRequest(this, completable, "id_enter_pin_on_jade");
            }

            try {
                // Authenticate with pinserver (loop/retry on failure)
                // Note: this should be a no-op if the user is already authenticated on the device.
                while (!this.jade.authUser(network.getCanonicalNetworkId())) {
                    Log.w(TAG, "Jade authentication failed");
                }
                completable.onComplete();
            }catch (Exception e){
                completable.onError(e);
                throw e;
            }

        }
        return true;
    }

    // Authenticate Jade with pinserver and check firmware version with fw-server
    public Single<JadeHWWallet> authenticate(final Network network, final HWWalletBridge hwWalletBridge, final JadeFirmwareManager jadeFirmwareManager) throws Exception {
        /*
         * 1. check firmware (and maybe OTA) any completely uninitialised device (ie no keys/pin set - no unlocking needed)
         * 2. authenticate the user (see above)
         * 3. check the firmware again (and maybe OTA) for devices that are set-up (and hence needed unlocking first)
         * 4. authenticate the user *if required* - as we may have OTA'd and rebooted the hww.  Should be a no-op if not needed.
         */
        return Single.just(this)
                .flatMap(hww -> jadeFirmwareManager.checkFirmware(jade, false))
                .map(fwValid -> authUser(network, hwWalletBridge))
                .flatMap(authed -> jadeFirmwareManager.checkFirmware(jade, true))
                .flatMap(fwValid -> {
                    if (fwValid) {
                        authUser(network, hwWalletBridge);  // re-auth if required
                        return Single.just(this);
                    } else {
                        return Single.error(new JadeError(JadeError.UNSUPPORTED_FIRMWARE_VERSION,
                                "Insufficient/invalid firmware version", null));
                    }
                });
    }

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
        // return signed.stream().map(Integer::toUnsignedLong).collect(Collectors.toList());
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

    @Override
    public List<String> getXpubs(final Network network, final HWWalletBridge parent, final List<List<Integer>> paths) {
        Log.d(TAG, "getXpubs() for " + paths.size() + " paths.");

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

    @Override
    public SignMsgResult signMessage(final HWWalletBridge parent, final List<Integer> path, final String message,
                                     final boolean useAeProtocol, final String aeHostCommitment, final String aeHostEntropy) {
        Log.d(TAG, "signMessage() for message of length " + message.length() + " using path " + path);

        CompletableSubject completable = CompletableSubject.create();

        try {
            final List<Long> unsignedPath = getUnsignedPath(path);

            if (parent != null) {
                parent.interactionRequest(this, completable, "id_check_device");
            }

            final SignMessageResult result = this.jade.signMessage(unsignedPath, message, useAeProtocol,
                                                                   hexToBytes(aeHostCommitment),
                                                                   hexToBytes(aeHostEntropy));
            completable.onComplete();

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
            return new SignMsgResult(sigDerHex, hexFromBytes(result.getSignerCommitment()));
        } catch (final Exception e) {
            completable.onError(e);
            throw new RuntimeException(e);
        }
    }

    // Helper to get the change paths for auto-validation
    private static List<TxChangeOutput> getChangeData(final List<InputOutput> outputs) {
        // Get the change outputs and paths
        final List<TxChangeOutput> change = new ArrayList<>(outputs.size());
        for (final InputOutput output : outputs) {
            if (output.isChange()) {
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

    @Override
    public SignTxResult signTransaction(final Network network, final HWWalletBridge parent,
                                        final ObjectNode tx,
                                        final List<InputOutput> inputs,
                                        final List<InputOutput> outputs,
                                        final Map<String, String> transactions,
                                        final boolean useAeProtocol) {
        Log.d(TAG, "signTransaction() called for " + inputs.size() + " inputs");
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
            final String txhex = tx.get("transaction").asText();
            final byte[] txn = hexToBytes(txhex);
            final SignTxInputsResult result = this.jade.signTx(canonicalNetworkId, useAeProtocol, txn, txInputs, change);

            Log.d(TAG, "signTransaction() returning " + result.getSignatures().size() + " signatures");
            return new SignTxResult(hexFromBytes(result.getSignatures()), hexFromBytes(result.getSignerCommitments()));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper to get the commitment and blinding key from Jade
    private Commitment getTrustedCommitment(final int index,
                                            final InputOutput output,
                                            final byte[] hashPrevOuts,
                                            final byte[] customVbf) throws IOException {
        final byte[] assetId = hexToBytes(output.getAssetId());
        final Commitment commitment = this.jade.getCommitments(
            assetId,
            output.getSatoshi(),
            hashPrevOuts,
            index,
            customVbf);

        // Add the script blinding key
        final byte[] blindingKey = hexToBytes(output.getBlindingKey());
        commitment.setBlindingKey(blindingKey);

        return commitment;
    }

    // Helper to pivot commitments and signatures into result structure
    private static SignTxResult createResult(final List<Commitment> commitments, final SignTxInputsResult signResult) {
        final List<String> assetGenerators = new ArrayList<>(commitments.size());
        final List<String> valueCommitments = new ArrayList<>(commitments.size());
        final List<String> abfs = new ArrayList<>(commitments.size());
        final List<String> vbfs = new ArrayList<>(commitments.size());

        for (final Commitment commitment : commitments) {
            // un-blinded output
            if (commitment == null || commitment.getAssetId() == null) {
                assetGenerators.add(null);
                valueCommitments.add(null);
                abfs.add(null);
                vbfs.add(null);
            } else {
                // Blinded output, populate commitments & blinding factors
                final String assetGenerator = hexFromBytes(commitment.getAssetGenerator());
                assetGenerators.add(assetGenerator);

                final String valueCommitment = hexFromBytes(commitment.getValueCommitment());
                valueCommitments.add(valueCommitment);

                final String abf = hexFromBytes(ExtensionsKt.reverseBytes(commitment.getAbf()));
                abfs.add(abf);

                final String vbf = hexFromBytes(ExtensionsKt.reverseBytes(commitment.getVbf()));
                vbfs.add(vbf);
            }
        }

        final List<String> signatures = hexFromBytes(signResult.getSignatures());
        final List<String> signerCommitments = hexFromBytes(signResult.getSignerCommitments());

        return new SignTxResult(signatures, signerCommitments, assetGenerators, valueCommitments, abfs, vbfs);
    }

    private static byte[] valueToLE(final int i) {
        final ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(i);
        return buf.array();
    }

    // Helper to flatten a list of byte arrays into one large byte array
    private static byte[] flatten(final List<byte[]> arrays) {
        if (arrays.isEmpty()) {
            return new byte[0];
        }

        final int size_estimate = arrays.get(0).length * arrays.size();
        final ByteArrayOutputStream output = new ByteArrayOutputStream(size_estimate);
        for (final byte[] chunk : arrays) {
            output.write(chunk, 0, chunk.length);
        }
        return output.toByteArray();
    }

    @Override
    public SignTxResult signLiquidTransaction(final Network network, final HWWalletBridge parent, final ObjectNode tx,
                                              final List<InputOutput> inputs,
                                              final List<InputOutput> outputs,
                                              final Map<String,String> transactions,
                                              final boolean useAeProtocol) {
        Log.d(TAG, "signLiquidTransaction() called for " + inputs.size() + " inputs");
        try {
            final int combinedSize = inputs.size() + outputs.size();
            final List<byte[]> inputPrevouts = new ArrayList<>(2*inputs.size());
            final List<Long> values = new ArrayList<>(combinedSize);
            final List<byte[]> abfs = new ArrayList<>(combinedSize);
            final List<byte[]> vbfs = new ArrayList<>(combinedSize);

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

                // Get values, abfs and vbfs from inputs (needed to compute the final output vbf)
                values.add(input.getSatoshi());
                abfs.add(input.getAbfs());
                vbfs.add(input.getVbfs());

                // Get the input prevout txid and index for hashing later
                inputPrevouts.add(input.getTxid());
                inputPrevouts.add(valueToLE(input.getPtIdxInt()));
            }

            // Compute the hash of all input prevouts for making deterministic blinding factors
            final byte[] hashPrevOuts = Wally.sha256d(flatten(inputPrevouts));

            // Get trusted commitments per output - null for unblinded outputs
            final List<Commitment> trustedCommitments = new ArrayList<>(outputs.size());

            // For all-but-last blinded entry, do not pass custom vbf, so one is generated
            // Append the output abfs and vbfs to the arrays
            // FIXME: assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
            final int lastBlindedIndex = outputs.size()-2;  // Could determine this properly
            for (int i = 0; i < lastBlindedIndex; ++i) {
                final InputOutput output = outputs.get(i);
                final Commitment commitment = getTrustedCommitment(i, output, hashPrevOuts, null);
                trustedCommitments.add(commitment);

                values.add(output.getSatoshi());
                abfs.add(commitment.getAbf());
                vbfs.add(commitment.getVbf());
            }

            // For the last blinded output, get the abf only
            final InputOutput lastBlindedOutput = outputs.get(lastBlindedIndex);
            values.add(lastBlindedOutput.getSatoshi());
            final byte[] lastAbf = this.jade.getBlindingFactor(hashPrevOuts, lastBlindedIndex, "ASSET");
            abfs.add(lastAbf);

            // For the last blinded output we need to calculate the correct vbf so everything adds up
            final byte[] lastVbf = Wally.asset_final_vbf(Longs.toArray(values), inputs.size(),
                                                         flatten(abfs), flatten(vbfs));
            vbfs.add(lastVbf);

            // Fetch the last commitment using that explicit vbf
            final Commitment lastCommitment = getTrustedCommitment(
                lastBlindedIndex,
                lastBlindedOutput,
                hashPrevOuts,
                lastVbf);
            trustedCommitments.add(lastCommitment);

            // Add a 'null' commitment for the final (fee) output
            trustedCommitments.add(null);

            // Get the change outputs and paths
            final List<TxChangeOutput> change = getChangeData(outputs);

            // Make jade-api call to sign the txn
            final String canonicalNetworkId = network.getCanonicalNetworkId();
            final String txhex = tx.get("transaction").asText();
            final byte[] txn = hexToBytes(txhex);
            final SignTxInputsResult result = this.jade.signLiquidTx(canonicalNetworkId, useAeProtocol, txn,
                                                                     txInputs, trustedCommitments, change);
            // Pivot data into return structure
            Log.d(TAG, "signLiquidTransaction() returning " + result.getSignatures().size() + " signatures");
            return createResult(trustedCommitments, result);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getMasterBlindingKey(HWWalletBridge parent) {
        Log.d(TAG, "getMasterBlindingKey() called");

        try {
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
    }

    @Override
    public String getBlindingKey(final HWWalletBridge parent, final String scriptHex) {
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

    @Override
    public String getBlindingNonce(final HWWalletBridge parent, final String pubkey, final String scriptHex) {
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

    @Override
    public String getGreenAddress(final Network network, final SubAccount subaccount, final List<Long> path, final long csvBlocks) {
        try {
            final String canonicalNetworkId = network.getCanonicalNetworkId();
            if (network.isMultisig()) {
                // Green Multisig Shield - pathlen should be 2 for subact 0, and 4 for subact > 0
                // In any case the last two entries are 'branch' and 'pointer'
                final int pathlen = path.size();
                final long branch = path.get(pathlen - 2);
                final long pointer = path.get(pathlen - 1);
                String recoveryxpub = null;

                Log.d(TAG,"getGreenAddress() (multisig shield) for subaccount: " + subaccount.getPointer() + ", branch: "
                        + branch + ", pointer " + pointer);

                // Jade expects any 'recoveryxpub' to be at the subact/branch level, consistent with tx outputs - but gdk
                // subaccount data has the base subaccount chain code and pubkey - so we apply the branch derivation here.
                if (subaccount.getRecoveryChainCode() != null && subaccount.getRecoveryChainCode().length() > 0) {
                    final Object subactkey = Wally.bip32_pub_key_init(
                            network.getVerPublic(), 0, 0,
                            subaccount.getRecoveryChainCodeAsBytes(), subaccount.getRecoveryPubKeyAsBytes());
                    final Object branchkey = Wally.bip32_key_from_parent(subactkey, branch,
                            Wally.BIP32_FLAG_KEY_PUBLIC |
                                    Wally.BIP32_FLAG_SKIP_HASH);
                    recoveryxpub = Wally.bip32_key_to_base58(branchkey, Wally.BIP32_FLAG_KEY_PUBLIC);
                    Wally.bip32_key_free(branchkey);
                    Wally.bip32_key_free(subactkey);
                }

                // Get receive address from Jade for the path elements given
                final String address = this.jade.getReceiveAddress(canonicalNetworkId,
                        subaccount.getPointer(), branch, pointer,
                        recoveryxpub, csvBlocks);
                Log.d(TAG, "Got green address for branch: " + branch + ", pointer: " + pointer + ": " + address);
                return address;
            } else {
                // Green Electrum Singlesig
                Log.d(TAG,"getGreenAddress() (singlesig) for path: " + path);
                final String variant = mapAddressType(subaccount.getType().getGdkType());
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
    public PublishSubject<Boolean> getBleDisconnectEvent() {
        return jade.getBleDisconnectEvent();
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.blockstream_jade_device;
    }
}
