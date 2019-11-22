package com.greenaddress.jade;

import android.util.Log;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.jade.entities.JadeError;
import com.greenaddress.jade.entities.Commitment;
import com.greenaddress.jade.entities.TxChangeOutput;
import com.greenaddress.jade.entities.TxInput;
import com.greenaddress.jade.entities.TxInputLiquid;
import com.greenaddress.jade.entities.VersionInfo;
import com.polidea.rxandroidble2.RxBleDevice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// GDKSession used to implement http-request calls during pinserver handshake
import static com.greenaddress.greenapi.Session.getSession;

/**
 * High-Level Synchronous Jade Client API
 * Builds on a JadeInterface to provide a properly typed API
 */
public class JadeAPI {
    private static final String TAG = "JadeAPI";

    // Timeouts for autonomous calls that should return quickly, calls that require user confirmation,
    // and calls that need arbitrarily long (eg. entering a mnemonic) and should not timeout at all.
    private static int TIMEOUT_AUTONOMOUS = 2000;  // 2 secs
    private static int TIMEOUT_USER_INTERACTION = 120000;  // 2 mins
    private static int TIMEOUT_NONE = -1;

    private final JadeInterface jade;
    private final Random idgen;
    private String efusemac;

    private JadeAPI(final JadeInterface jade) {
        this.jade = jade;
        this.idgen = new Random();
        this.efusemac = null;
    }

    public static JadeAPI createSerial(final UsbManager usbManager, final UsbDevice usbDevice, final int baud) {
        final JadeInterface jade = JadeInterface.createSerial(usbManager, usbDevice, baud);
        return new JadeAPI(jade);
    }

    public static JadeAPI createBle(final RxBleDevice device) {
        final JadeInterface jade = JadeInterface.createBle(device);
        return new JadeAPI(jade);
    }

    public boolean connect() {
        // Connect the underlying transport
        this.jade.connect();

        // Test/flush the connection for a limited time
        for (int attempt = 4; attempt >= 0; --attempt) {
            // Short sleep before (re-)trying connection)
            android.os.SystemClock.sleep(1000);

            try {
                this.jade.drain();
                final VersionInfo info = this.getVersionInfo();
                this.efusemac = info.getEfusemac();
                return true;
            } catch (final Exception e) {
                // On error loop trying again
                Log.w(TAG, "Error trying connect: " + e);
            }
        }

        // Couldn't verify connection
        Log.e(TAG,"Exhausted retries, failed to connect to Jade");
        return false;
    }

    public void disconnect() {
        this.jade.disconnect();
        this.efusemac = null;
    }

    // Helper to create single-parameter object
    private static ObjectNode makeParams(final String name, final Object val) {
        final ObjectNode params = JadeInterface.mapper().createObjectNode();
        params.set(name, JadeInterface.mapper().valueToTree(val));
        return params;
    }

    // Helper to raise any returned error as an exception
    private static JsonNode getResultOrRaiseError(final JsonNode response, final String verify_id) {
        final JsonNode error = response.get("error");
        if (error != null) {
            throw new JadeError(error.get("code").asInt(),
                                error.get("message").asText(),
                                error.get("data"));
        } else if (verify_id != null && (response.get("id") == null || !verify_id.equals(response.get("id").asText()))) {
            throw new JadeError(10,
                    "Request/Response id mismatch - expected id: " + verify_id,
                    response.get("id"));
        }

        return response.get("result");
    }

    // Helper to build an request to send
    private static JsonNode buildRequest(final String id, final String method, final JsonNode params) {
        final ObjectNode root = JadeInterface.mapper().createObjectNode()
                .put("method", method)
                .put("id", id);

        if (params != null) {
            root.set("params",  params);
        }
        return root;
    }

    // Helper to make http requests (with retries)
    // NOTE: Uses GDKSession's httpRequest() call to ensure Tor use as appropriate.
    private static JsonNode makeHttpRequest(final JsonNode request) throws IOException {

        // If it fails retry up to 3 times
        for (int attempt = 2; attempt >= 0; --attempt) {
            Log.i(TAG,"Making gdk http request: " + request.toString());
            final JsonNode response = getSession().httpRequest(request);
            Log.i(TAG,"Received gdk http response: " + response.toString());

            // Return the 'body' if received
            final JsonNode body = response.get("body");
            if (body != null) {
                return body;
            }
            Log.e(TAG,"No response body received!");

            // Short sleep before retry
            if (attempt > 0)
                android.os.SystemClock.sleep(500);
        }
        Log.e(TAG,"Exhausted retries");
        return null;
    }

    // Helper to call wrapper interface rpc invoker
    private JsonNode jadeRpc(final String method, final JsonNode params, final String id, final int timeout) throws IOException {
        final String newid = id != null ? id : String.valueOf(100000 + this.idgen.nextInt(899999));
        final JsonNode request = buildRequest(newid, method, params);
        final JsonNode response = this.jade.makeRpcCall(request, timeout);
        final JsonNode result = getResultOrRaiseError(response, newid);

        /*
         * The Jade can respond with a request for interaction with a remote
         * http server. This is used for interaction with the pinserver but the
         * code below acts as a dumb proxy and simply makes the http request and
         * forwards the response back to the Jade.
         */
        if (result.isObject() && result.has("http_request")) {
            final JsonNode httpRequest = result.get("http_request");
            final String onHttpReplyCall = httpRequest.get("on-reply").asText();

            final JsonNode httpParams = httpRequest.get("params");
            final JsonNode httpResponse = makeHttpRequest(httpParams);
            return this.jadeRpc(onHttpReplyCall, httpResponse, timeout);
        }

        return result;
    }

    private JsonNode jadeRpc(final String method, final JsonNode params, final int timeout) throws IOException {
        return this.jadeRpc(method, params, null, timeout);
    }

    private JsonNode jadeRpc(final String method, final int timeout) throws IOException {
        return this.jadeRpc(method, null, timeout);
    }

    // Test connection
    // FIXME: imho Jade really needs a dedicated connection-test call that is answered immediately
    // by the network thread which receives it, and not by the dashboard thread (which may be busy)
    private void testConnection() throws IOException {
        final VersionInfo info = getVersionInfo();
        if (info == null || info.getEfusemac() == null || !info.getEfusemac().equals(this.efusemac)) {
            throw new IOException("Lost Jade connection");
        }
    }

    // Get version information from the jade
    public VersionInfo getVersionInfo() throws IOException {
        final JsonNode result = this.jadeRpc("get_version_info", TIMEOUT_AUTONOMOUS);
        return JadeInterface.mapper().treeToValue(result, VersionInfo.class);
    }

    // Send additional entropy for the rng to jade
    public boolean addEntropy(final byte[] entropy) throws IOException {
        final JsonNode params = makeParams("entropy", entropy);
        final JsonNode result = this.jadeRpc("add_entropy", params, TIMEOUT_AUTONOMOUS);
        return result.asBoolean();
    }

    // Callback user can supply to be called after each chunk is uploaded
    public interface  OtaProgressCallback {
        void invoke(final int written, final int totalsize);
    }

    // OTA firmware update
    public boolean otaUpdate(final byte[] compressed_firmware,
                             final int uncompressed_size,
                             final int chunksize,
                             final OtaProgressCallback cb) throws Exception {

        final int compressed_size = compressed_firmware.length;

        // Initiate OTA
        final JsonNode result = this.jadeRpc("ota",
                makeParams("fwsize", uncompressed_size)
                    .put("cmpsize", compressed_size)
                    .put("otachunk", chunksize),
                TIMEOUT_AUTONOMOUS);

        if (!result.asBoolean()) {
            return false;
        }

        // Write binary chunks
        final JsonNodeFactory nodeFactory = JadeInterface.mapper().getNodeFactory();
        int written = 0;
        while (written < compressed_size) {
            final int remaining = compressed_size - written;
            final int length = Math.min(remaining, chunksize);
            final JsonNode chunk = nodeFactory.binaryNode(compressed_firmware, written, length);
            this.jadeRpc("ota_data", chunk, String.valueOf(written+length), TIMEOUT_USER_INTERACTION);
            written += length;

            // Call progress callback
            if (cb != null) {
                cb.invoke(written, compressed_size);
            }
        }

        final JsonNode status = this.jadeRpc("ota_complete", TIMEOUT_AUTONOMOUS);
        return status.asBoolean();
    }

    // Trigger user authentication on the hw
    // Involves pinserver handshake
    public boolean authUser(final String network) throws IOException {
        //testConnection(); - // FIXME: if/when have better connection-test message that does not rely on dashboard responding
        final JsonNode params = makeParams("network", network);
        final JsonNode result = this.jadeRpc("auth_user", params, TIMEOUT_NONE);
        return result.asBoolean();
    }

    // Get (receive) green address
    public String getReceiveAddress(final String network, final long subaccount, final long branch, final long pointer, final String recoveryxpub, final Long csvBlocks) throws IOException {
        testConnection();
        final JsonNode params = makeParams("network", network)
                .put("subaccount", subaccount)
                .put("branch", branch)
                .put("pointer", pointer)
                .put("recovery_xpub", recoveryxpub)
                .put("csv_blocks", csvBlocks);
        final JsonNode result = this.jadeRpc("get_receive_address", params, TIMEOUT_USER_INTERACTION);
        return result.asText();
    }

    // Get xpub given path
    public String getXpub(final String network, final List<Long> path) throws IOException {
        final JsonNode params = makeParams("path", path).put("network", network);
        final JsonNode result = this.jadeRpc("get_xpub", params, TIMEOUT_AUTONOMOUS);
        return result.asText();
    }

    // Sign a message
    public String signMessage(final List<Long> path, final String message) throws IOException {
        testConnection();
        final JsonNode params = makeParams("path", path).put("message", message);
        final JsonNode result = this.jadeRpc("sign_message", params, TIMEOUT_USER_INTERACTION);
        return result.asText();
    }

    // Helper to send transaction inputs and retrieve signature responses
    private List<byte[]> signTxInputs(final int baseId, final List<? extends TxInput> inputs) throws IOException {
        // Send all n inputs
        int i = 0;
        for (final TxInput input : inputs) {
            final String id = String.valueOf(baseId + i + 1);
            ++i;

            final JsonNode inputParams = JadeInterface.mapper().valueToTree(input);
            final JsonNode request = buildRequest(id, "tx_input", inputParams);
            this.jade.writeRequest(request);

            // FIXME - pause to not flood buffers
            android.os.SystemClock.sleep(100);
        }

        // Receive all n signatures
        final List<byte[]> signatures = new ArrayList<>(inputs.size());
        for (i = 0; i < inputs.size(); ++i) {
            final String id = String.valueOf(baseId + i + 1);

            final JsonNode response = this.jade.readResponse(TIMEOUT_USER_INTERACTION);
            final JsonNode signatureResult = getResultOrRaiseError(response, id);
            signatures.add(signatureResult.binaryValue());
        }

        return signatures;
    }

    // Sign a transaction
    public List<byte[]> signTx(final String network, final byte[] txn, final List<TxInput> inputs, final List<TxChangeOutput> change) throws IOException {
        /**
         * Protocol:
         * 1st message contains txn and number of inputs we are going to send.
         * Reply ok if that corresponds to the expected number of inputs (n).
         * Then we send one message per input - without expecting replies.
         * Once all n input messages are sent, the hw then sends all n replies
         * (as the user has a chance to confirm/cancel at this point).
         * Then receive all n replies for the n signatures.
         * NOTE: *NOT* a sequence of n blocking rpc calls.
         */
        testConnection();
        final int baseId = 100 * (1000 + this.idgen.nextInt(8999));
        final JsonNode params = makeParams("change", change)
                .put("network", network)
                .put("num_inputs", inputs.size())
                .put("txn", txn);

        final JsonNode result = this.jadeRpc("sign_tx", params, String.valueOf(baseId), TIMEOUT_USER_INTERACTION);
        if (!result.asBoolean()) {
            throw new JadeError(11, "Error response from initial sign_tx call", result);
        }

        // Use helper function to send inputs and process replies
        return signTxInputs(baseId, inputs);
    }

    // Liquid calls

    // Get blinding key for script
    public byte[] getBlindingKey(final byte[] script) throws IOException {
        final JsonNode params = makeParams("script", script);
        final JsonNode result = this.jadeRpc("get_blinding_key", params, TIMEOUT_AUTONOMOUS);
        return result.binaryValue();
    }

    // Get the shared secret to unblind a tx, given the receiving script on our side
    // and the pubkey of the sender (sometimes called "nonce" in Liquid)
    public byte[] getSharedNonce(final byte[] script, final byte[] pubkey) throws IOException {
        final JsonNode params = makeParams("script", script).put("their_pubkey", pubkey);
        final JsonNode result = this.jadeRpc("get_shared_nonce", params, TIMEOUT_AUTONOMOUS);
        return result.binaryValue();
    }

    // Get a "trusted" blinding factor to blind an output. Normally the blinding
    // factors are generated and returned in the `get_commitments` call, but
    // for the last output the VBF must be generated on the host side, so this
    //  call allows the host to get a valid ABF to compute the generator and
    // then the "final" VBF. Nonetheless, this call is kept generic, and can
    // also generate VBFs, thus the "type" parameter.
    // `hash_prevouts` is computed as specified in BIP143 (double SHA of all
    //   the outpoints being spent as input. It's not checked right away since
    //   at this point Jade doesn't know anything about the tx we are referring
    //   to. It will be checked later during `sign_liquid_tx`.
    // `output_index` is the output we are trying to blind.
    // `type` can either be "ASSET" or "VALUE" to generate ABFs or VBFs.
    public byte[] getBlindingFactor(final byte[] hashPrevouts, final Integer outputIdx, final String type) throws IOException {
        final JsonNode params = makeParams("hash_prevouts", hashPrevouts)
                .put("output_index", outputIdx)
                .put("type", type);
        final JsonNode result = this.jadeRpc("get_blinding_factor", params, TIMEOUT_AUTONOMOUS);
        return result.binaryValue();
    }

    // Generate the blinding factors and commitments for a given output.
    // Can optionally get a "custom" VBF, normally used for the last
    // input where the VBF is not random, but generated accordingly to
    // all the others.
    // `hash_prevouts` and `output_index` have the same meaning as in
    //   the `get_blinding_factor` call.
    // NOTE: the `asset_id` should be passed as it is normally displayed, so
    // reversed compared to the "consensus" representation.
    public Commitment getCommitments(final byte[] assetId,
                                     final Long value,
                                     final byte[] hashPrevouts,
                                     final Integer outputIdx,
                                     final byte[] vbf) throws IOException {
        final ObjectNode params = makeParams("hash_prevouts", hashPrevouts)
                .put("output_index", outputIdx)
                .put("asset_id", assetId)
                .put("value", value);

        // Optional vbf param
        if (vbf != null) {
            params.put("vbf", vbf);
        }

        final JsonNode result = this.jadeRpc("get_commitments", params, TIMEOUT_AUTONOMOUS);
        return JadeInterface.mapper().treeToValue(result, Commitment.class);
    }

    // Sign a liquid transaction
    public List<byte[]> signLiquidTx(final String network,
                                     final byte[] txn,
                                     final List<TxInputLiquid> inputs,
                                     final List<Commitment> trustedCommitments,
                                     final List<TxChangeOutput> change) throws IOException {
        /**
         * Protocol:
         * 1st message contains txn and number of inputs we are going to send.
         * Reply ok if that corresponds to the expected number of inputs (n).
         * Then we send one message per input - without expecting replies.
         * Once all n input messages are sent, the hw then sends all n replies
         * (as the user has a chance to confirm/cancel at this point).
         * Then receive all n replies for the n signatures.
         * NOTE: *NOT* a sequence of n blocking rpc calls.
         */
        testConnection();
        final int baseId = 100 * (1000 + this.idgen.nextInt(8999));
        final ObjectNode params = makeParams("change", change)
                .put("network", network)
                .put("num_inputs", inputs.size());

        // Add the trusted commitments
        final ArrayNode allCommitments = params.putArray("trusted_commitments");
        for (final Commitment commit : trustedCommitments) {
            final JsonNode commitParams = JadeInterface.mapper().valueToTree(commit);
            allCommitments.add(commitParams);
        }

        // Add the tx last
        params.put("txn", txn);

        final JsonNode result = this.jadeRpc("sign_liquid_tx", params, String.valueOf(baseId), TIMEOUT_USER_INTERACTION);
        if (!result.asBoolean()) {
            throw new JadeError(11,"Error response from initial sign_liquid_tx call", result);
        }

        // Use helper function to send inputs and process replies
        return signTxInputs(baseId, inputs);
    }
}
