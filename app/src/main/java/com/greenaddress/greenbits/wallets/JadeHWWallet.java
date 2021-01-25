package com.greenaddress.greenbits.wallets;

import android.content.Context;
import android.content.res.Resources;
import android.util.Base64;
import android.util.Log;

import com.blockstream.libgreenaddress.GDK;
import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CharStreams;
import com.google.common.primitives.Longs;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.InputOutputData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.jade.JadeAPI;
import com.greenaddress.jade.entities.Commitment;
import com.greenaddress.jade.entities.JadeError;
import com.greenaddress.jade.entities.TxChangeOutput;
import com.greenaddress.jade.entities.TxInput;
import com.greenaddress.jade.entities.TxInputBtc;
import com.greenaddress.jade.entities.TxInputLiquid;
import com.greenaddress.jade.entities.VersionInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.greenaddress.greenapi.data.InputOutputData.reverseBytes;

// GDKSession used to implement http-request calls to firmware-server
import static com.greenaddress.greenapi.Session.getSession;

public class JadeHWWallet extends HWWallet {
    private static final String TAG = "JadeHWWallet";

    private static final String JADE_MIN_ALLOWED_FW_VERSION = "0.1.21";

    private static final String JADE_FW_SERVER_HTTPS = "https://jadefw.blockstream.com/bin/";
    private static final String JADE_FW_SERVER_ONION = "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion/bin/";
    private static final String JADE_FW_VERSIONS_FILE = "LATEST";
    private static final String JADE_FW_SUFFIX = "fw.bin";

    private final Resources res;
    private final JadeAPI jade;

    public JadeHWWallet(final Context ctx, final JadeAPI jade, final NetworkData network, final HWDeviceData hwDeviceData) {

        super.mNetwork = network;
        super.mHWDeviceData = hwDeviceData;

        this.res = ctx.getResources();
        this.jade = jade;
    }

    @Override
    public void disconnect() {
        this.jade.disconnect();
    }

    public void authenticate() throws Exception {
        final VersionInfo verInfo = this.jade.getVersionInfo();
        if (!verInfo.getHasPin()) {
            // Do firmware check / ota immediately
            checkAndUpdateFirmware(jade);
        }

        boolean bUpdated = false;
        do {
            // Send some entropy from GDK into Jade
            this.jade.addEntropy(GDK.get_random_bytes(32));

            // Authenticate with pinserver (loop/retry on failure)
            while (!this.jade.authUser(getNetwork().getNetwork())) {
                Log.w(TAG, "Jade authentication failed");
            }

            // Do firmware check / ota after login
            bUpdated = checkAndUpdateFirmware(jade);
        } while (bUpdated);
    }

    // Check Jade fw against minimum allowed firmware version
    private static boolean isJadeFwValid(final String version) {
        return JADE_MIN_ALLOWED_FW_VERSION.compareTo(version) <= 0;
    }

    // Uses GDKSession's httpRequest() to get file from Jade firmware server - ensures Tor use as appropriate.
    private byte[] downloadJadeFwFile(final String fwFileName, final boolean isBase64) throws IOException {
        final URL tls = new URL(JADE_FW_SERVER_HTTPS + fwFileName);
        final URL onion = new URL(JADE_FW_SERVER_ONION + fwFileName);
        final String certificate = CharStreams.toString(new InputStreamReader(
                                                            this.res.openRawResource(R.raw.jade_services_certificate),
                                                            Charsets.UTF_8));

        // Make http GET call to fetch file
        final JsonNode ret = getSession().httpRequest("GET",
                                                      Arrays.asList(tls, onion),
                                                      null,
                                                      isBase64 ? "base64" : "text",
                                                      Collections.singletonList(certificate));

        if (ret == null || !ret.has("body")) {
            throw new IOException("Failed to fetch firmware file: " + fwFileName);
        }

        final String body = ret.get("body").asText();
        return isBase64 ? Base64.decode(body, Base64.DEFAULT) : body.getBytes();
    }

    private void doJadeOtaUpdate(final JadeAPI jade, final VersionInfo verInfo) throws Exception {
        final String config = verInfo.getJadeConfig().toLowerCase();
        final int chunksize = verInfo.getJadeOtaMaxChunk();

        final byte[] versions = downloadJadeFwFile(JADE_FW_VERSIONS_FILE, false);
        final String files[] = new String(versions).split("\n");
        final String substr = "_" + config + "_";
        String fwFilename = null;
        for (final String f : files) {
            if (f.endsWith(JADE_FW_SUFFIX) && f.contains(substr)) {
                fwFilename = f;
                break;
            }
        }

        if (fwFilename == null) {
            throw new RuntimeException("Error scanning LATEST versions file");
        }

        // Split the filename, verify format, extract version and size
        final String parts[] = fwFilename.split("_");
        if (parts.length != 4 || !config.equals(parts[1]) || !JADE_FW_SUFFIX.equals(parts[3])) {
            throw new RuntimeException("Unexpected firmware filename: " + fwFilename);
        }
        final String version = parts[0];
        final int fullsize = Integer.parseInt(parts[2]);

        // Do not downgrade/reinstall
        if (version.compareTo(verInfo.getJadeVersion()) <= 0) {
            Log.w(TAG, "New firmware appears to be same/downgrade - ignoring.");
            Log.w(TAG, "Installed version: " + verInfo.getJadeVersion());
            Log.w(TAG, "Available version: " + version);
            return;
        }

        // Log if upgrade but still insufficient to satisfy minimum version
        if (!isJadeFwValid(version)) {
            Log.w(TAG, "New firmware does not appear sufficient to satisfy required minimum version.");
            Log.w(TAG, "Allowed minumum: " + JADE_MIN_ALLOWED_FW_VERSION);
            Log.w(TAG, "Available version: " + version);
        }

        // Copy file from fw server
        Log.i(TAG, "Fetching firmware file: " + fwFilename);
        final byte[] firmware = downloadJadeFwFile(fwFilename, true);

        // Call jade ota update
        Log.i(TAG, "Uploading firmware file: " + fwFilename + ", size: " + firmware.length);
        jade.otaUpdate(firmware, fullsize, chunksize, null);
    }

    private boolean checkAndUpdateFirmware(final JadeAPI jade) throws Exception {
        // Do firmware check and ota if necessary
        final VersionInfo verInfo = this.jade.getVersionInfo();
        if (isJadeFwValid(verInfo.getJadeVersion())) {
            // No update required
            return false;
        }

        // OTA
        doJadeOtaUpdate(jade, verInfo);
        jade.disconnect();

        // Sleep to allow Jade to reboot, then retry/reconnect
        android.os.SystemClock.sleep(6000);
        if (!jade.connect()) {
            throw new RuntimeException("Failed to reconnect to Jade after OTA");
        }

        return true;
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
    public List<String> getXpubs(final GaActivity parent, final List<List<Integer>> paths) {
        Log.d(TAG, "getXpubs() for " + paths.size() + " paths.");

        final String network = getNetwork().getNetwork();
        try {
            // paths.stream.map(jade::get_xpub).collect(Collectors.toList());
            final List<String> xpubs = new ArrayList<>(paths.size());
            for (final List<Integer> path : paths) {
                final List<Long> unsignedPath = getUnsignedPath(path);
                final String xpub = this.jade.getXpub(network, unsignedPath);
                Log.d(TAG, "Got xpub for " + path + ": " + xpub);
                xpubs.add(xpub);
            }

            Log.d(TAG, "getXpubs() returning " + xpubs.size() + " xpubs");
            return xpubs;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String signMessage(final GaActivity parent, final List<Integer> path, final String message) {
        Log.d(TAG, "signMessage() for message of length " + message.length() + " using path " + path);

        try {
            final List<Long> unsignedPath = getUnsignedPath(path);
            final String sigEncoded = this.jade.signMessage(unsignedPath, message);
            final byte[] sigDecoded = BaseEncoding.base64().decode(sigEncoded);

            final byte[] sigDer = new byte[Wally.EC_SIGNATURE_DER_MAX_LEN];
            final int len = Wally.ec_sig_to_der(Arrays.copyOfRange(sigDecoded, 1, sigDecoded.length), sigDer);
            final String sigDerHex =  Wally.hex_from_bytes(Arrays.copyOfRange(sigDer, 0, len));

            Log.d(TAG, "signMessage() returning: " + sigDerHex);
            return sigDerHex;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper to get the change paths for auto-validation
    private static List<TxChangeOutput> getChangeData(final List<InputOutputData> outputs) {
        // Get the change outputs and paths
        final List<TxChangeOutput> change = new ArrayList<>(outputs.size());
        for (final InputOutputData output : outputs) {
            if (output.getIsChange()) {
                // change - get path
                change.add(new TxChangeOutput(output.getUserPath(),
                                              output.getRecoveryXpub(),
                                              "csv".equals(output.getAddressType()) ? output.getSubtype() : 0));
            } else {
                // Not change, put null place holder
                change.add(null);
            }
        }
        return change;
    }

    // Helper to map an array of n byte arrays into an array of n hex strings
    private static List<String> getHexFromBytes(final List<byte[]> lstByteArrays) {
        // return lstByteArrays.stream().map(Wally::hex_from_bytes).collect(Collectors.toList());
        final List<String> lstHexes = new ArrayList<>(lstByteArrays.size());
        for (final byte[] bytes : lstByteArrays) {
            final String hexstr = Wally.hex_from_bytes(bytes);
            lstHexes.add(hexstr);
        }
        return lstHexes;
    }

    @Override
    public List<String> signTransaction(final GaActivity parent,
                                        final ObjectNode tx,
                                        final List<InputOutputData> inputs,
                                        final List<InputOutputData> outputs,
                                        final Map<String, String> transactions,
                                        final List<String> addressTypes) {
        Log.d(TAG, "signTransaction() called for " + inputs.size() + " inputs");
        try {
            if (addressTypes.contains("p2pkh")) {
                throw new RuntimeException("Hardware Wallet cannot sign sweep inputs");
            }
            if (transactions == null || transactions.isEmpty()) {
                throw new RuntimeException("Input transactions missing");
            }

            // Get the inputs in the form Jade expects
            final List<TxInput> txInputs = new ArrayList<>(inputs.size());
            for (final InputOutputData input : inputs) {
                final boolean swInput = !input.getAddressType().equals("p2sh");

                // For a SegWit input we need to send prevout script explicitly
                final byte[] script = swInput ? Wally.hex_to_bytes(input.getPrevoutScript()) : null;

                if (swInput && inputs.size() == 1) {
                    // Single SegWit input - can skip sending entire tx and just send the sats amount
                    txInputs.add(new TxInputBtc(swInput,
                                                null,
                                                script,
                                                input.getSatoshi(),
                                                input.getUserPath()));
                } else {
                    // Non-SegWit input or there are several inputs - in which case we always send
                    // the entire prior transaction up to Jade (so it can verify the spend amounts).
                    final String txhex = transactions.get(input.getTxhash());
                    if (txhex == null) {
                        throw new RuntimeException("Required input transaction not found: " + input.getTxhash());
                    }
                    final byte[] inputTx = Wally.hex_to_bytes(txhex);
                    txInputs.add(new TxInputBtc(swInput,
                                                inputTx,
                                                script,
                                                null,
                                                input.getUserPath()));
                }
            }

            // Get the change outputs and paths
            final List<TxChangeOutput> change = getChangeData(outputs);

            // Make jade-api call
            final String network = getNetwork().getNetwork();
            final String txhex = tx.get("transaction").asText();
            final byte[] txn = Wally.hex_to_bytes(txhex);
            final List<byte[]> signatures = this.jade.signTx(network, txn, txInputs, change);

            final List<String> hexSigs = getHexFromBytes(signatures);
            Log.d(TAG, "signTransaction() returning " + hexSigs.size() + " signatures");
            return hexSigs;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper to get the commitment and blinding key from Jade
    private Commitment getTrustedCommitment(final int index,
                                            final InputOutputData output,
                                            final byte[] hashPrevOuts,
                                            final byte[] customVbf) throws IOException {
        final byte[] assetId = Wally.hex_to_bytes(output.getAssetId());
        final Commitment commitment = this.jade.getCommitments(
            assetId,
            output.getSatoshi(),
            hashPrevOuts,
            index,
            customVbf);

        // Add the script blinding key
        final byte[] blindingKey = Wally.hex_to_bytes(output.getPublicKey());
        commitment.setBlindingKey(blindingKey);

        return commitment;
    }

    // Helper to pivot commitments and signatures into result structure
    private static LiquidHWResult createResult(final List<Commitment> commitments, final List<String> signatures) {
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
                final String assetGenerator = Wally.hex_from_bytes(commitment.getAssetGenerator());
                assetGenerators.add(assetGenerator);

                final String valueCommitment = Wally.hex_from_bytes(commitment.getValueCommitment());
                valueCommitments.add(valueCommitment);

                final String abf = Wally.hex_from_bytes(reverseBytes(commitment.getAbf()));
                abfs.add(abf);

                final String vbf = Wally.hex_from_bytes(reverseBytes(commitment.getVbf()));
                vbfs.add(vbf);
            }
        }
        return new LiquidHWResult(signatures, assetGenerators, valueCommitments, abfs, vbfs);
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
    public LiquidHWResult signLiquidTransaction(final GaActivity parent, final ObjectNode tx,
                                                final List<InputOutputData> inputs,
                                                final List<InputOutputData> outputs,
                                                final Map<String,String> transactions,
                                                final List<String> addressTypes) {
        Log.d(TAG, "signLiquidTransaction() called for " + inputs.size() + " inputs");
        try {
            if (addressTypes.contains("p2pkh")) {
                throw new RuntimeException("Hardware Wallet cannot sign sweep inputs");
            }

            final int combinedSize = inputs.size() + outputs.size();
            final List<byte[]> inputPrevouts = new ArrayList<>(2*inputs.size());
            final List<Long> values = new ArrayList<>(combinedSize);
            final List<byte[]> abfs = new ArrayList<>(combinedSize);
            final List<byte[]> vbfs = new ArrayList<>(combinedSize);

            // Collect data from the tx inputs
            final List<TxInputLiquid> txInputs = new ArrayList<>(inputs.size());
            for (final InputOutputData input : inputs) {
                // Get the input in the form Jade expects
                final boolean swInput = !input.getAddressType().equals("p2sh");
                final byte[] script = Wally.hex_to_bytes(input.getPrevoutScript());
                final byte[] commitment = Wally.hex_to_bytes(input.getCommitment());
                txInputs.add(new TxInputLiquid(swInput,
                                               script,
                                               commitment,
                                               input.getUserPath()));

                // Get values, abfs and vbfs from inputs (needed to compute the final output vbf)
                values.add(input.getSatoshi());
                abfs.add(input.getAbfs());
                vbfs.add(input.getVbfs());

                // Get the input prevout txid and index for hashing later
                inputPrevouts.add(input.getTxid());
                inputPrevouts.add(valueToLE(input.getPtIdx().intValue()));
            }

            // Compute the hash of all input prevouts for making deterministic blinding factors
            final byte[] hashPrevOuts = Wally.sha256d(flatten(inputPrevouts));

            // Get trusted commitments per output - null for unblinded outputs
            final List<Commitment> trusted_commitments = new ArrayList<>(outputs.size());

            // For all-but-last blinded entry, do not pass custom vbf, so one is generated
            // Append the output abfs and vbfs to the arrays
            // FIXME: assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
            final int lastBlindedIndex = outputs.size()-2;  // Could determine this properly
            for (int i = 0; i < lastBlindedIndex; ++i) {
                final InputOutputData output = outputs.get(i);
                final Commitment commitment = getTrustedCommitment(i, output, hashPrevOuts, null);
                trusted_commitments.add(commitment);

                values.add(output.getSatoshi());
                abfs.add(commitment.getAbf());
                vbfs.add(commitment.getVbf());
            }

            // For the last blinded output, get the abf only
            final InputOutputData lastBlindedOutput = outputs.get(lastBlindedIndex);
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
            trusted_commitments.add(lastCommitment);

            // Add a 'null' commitment for the final (fee) output
            trusted_commitments.add(null);

            // Get the change outputs and paths
            final List<TxChangeOutput> change = getChangeData(outputs);

            // Make jade-api call to sign the txn
            final String network = getNetwork().getNetwork();
            final String txhex = tx.get("transaction").asText();
            final byte[] txn = Wally.hex_to_bytes(txhex);
            final List<byte[]> signatures =
                this.jade.signLiquidTx(network, txn, txInputs, trusted_commitments, change);

            // Pivot data into return structure
            final List<String> hexSigs = getHexFromBytes(signatures);
            Log.d(TAG, "signLiquidTransaction() returning " + signatures.size() + " signatures");
            return createResult(trusted_commitments, hexSigs);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBlindingKey(final GaActivity parent, final String scriptHex) {
        Log.d(TAG, "getBlindingKey() for script of length " + scriptHex.length());

        try {
            final byte[] script = Wally.hex_to_bytes(scriptHex);
            final byte[] bkey = this.jade.getBlindingKey(script);
            final String keyHex = Wally.hex_from_bytes(bkey);
            Log.d(TAG, "getBlindingKey() returning " + keyHex);
            return keyHex;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String getBlindingNonce(final GaActivity parent, final String pubkey, final String scriptHex) {
        Log.d(TAG, "getBlindingNonce() for script of length " + scriptHex.length() + " and pubkey " + pubkey);

        try {
            final byte[] script = Wally.hex_to_bytes(scriptHex);
            final byte[] pkey = Wally.hex_to_bytes(pubkey);
            final byte[] nonce = this.jade.getSharedNonce(script, pkey);
            final String nonceHex = Wally.hex_from_bytes(nonce);
            Log.d(TAG, "getBlindingNonce() returning " + nonceHex);
            return nonceHex;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String getGreenAddress(final SubaccountData subaccount, final long branch, final long pointer,
                                  final long csvBlocks) {
        Log.d(TAG,
              "getGreenAddress() for subaccount: " + subaccount.getPointer() + ", branch: " + branch + ", pointer " +
              pointer);

        try {
            final String network = getNetwork().getNetwork();
            String recoveryxpub = null;

            // Jade expects any 'recoveryxpub' to be at the subact/branch level, consistent with tx outputs - but gdk
            // subaccount data has the base subaccount chain code and pubkey - so we apply the branch derivation here.
            if (subaccount.getRecoveryChainCode() != null && subaccount.getRecoveryChainCode().length() > 0) {
                final Object subactkey = Wally.bip32_pub_key_init(
                    getNetwork().getVerPublic(), 0, 0,
                    subaccount.getRecoveryChainCodeAsBytes(), subaccount.getRecoveryPubKeyAsBytes());
                final Object branchkey = Wally.bip32_key_from_parent(subactkey, branch,
                                                                     Wally.BIP32_FLAG_KEY_PUBLIC |
                                                                     Wally.BIP32_FLAG_SKIP_HASH);
                recoveryxpub = Wally.bip32_key_to_base58(branchkey, Wally.BIP32_FLAG_KEY_PUBLIC);
                Wally.bip32_key_free(branchkey);
                Wally.bip32_key_free(subactkey);
            }

            // Get receive address from Jade for the path elements given
            final String address = this.jade.getReceiveAddress(network,
                                                               subaccount.getPointer(), branch, pointer,
                                                               recoveryxpub, csvBlocks);
            Log.d(TAG, "Got green address for branch: " + branch + ", pointer: " + pointer + ": " + address);
            return address;
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

    @Override
    public int getIconResourceId() {
        return R.drawable.ic_jade;
    }
}
