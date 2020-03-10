package com.greenaddress.greenbits.wallets;

import android.util.Log;

import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.InputOutputData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.satoshilabs.trezor.Trezor;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TrezorHWWallet extends HWWallet {

    private static final String TAG = TrezorHWWallet.class.getSimpleName();

    private final Trezor mTrezor;
    private final Map<String, TrezorType.HDNodeType> mUserXPubs = new HashMap<>();
    private final Map<String, TrezorType.HDNodeType> mServiceXPubs = new HashMap<>();
    private final Map<String, TrezorType.HDNodeType> mRecoveryXPubs = new HashMap<>();
    private final Map<String, Object> mPrevTxs = new HashMap<>();

    public TrezorHWWallet(final Trezor t, final NetworkData network, final HWDeviceData hwDeviceData) {
        mTrezor = t;
        mNetwork = network;
        mHWDeviceData = hwDeviceData;
    }

    @Override
    public List<String> getXpubs(final GaActivity parent, final List<List<Integer>> paths) {
        final List<String> xpubs = new ArrayList<>(paths.size());

        for (List<Integer> path : paths) {
            final TrezorType.HDNodeType xpub = getUserXpub(parent, path);

            final Object hdkey = Wally.bip32_pub_key_init(mNetwork.getVerPublic(),
                                                          1, 1, xpub.getChainCode().toByteArray(),
                                                          xpub.getPublicKey().toByteArray());

            xpubs.add(Wally.bip32_key_to_base58(hdkey, Wally.BIP32_FLAG_KEY_PUBLIC));
            Wally.bip32_key_free(hdkey);
        }
        return xpubs;
    }

    @Override
    public String signMessage(final GaActivity parent, final List<Integer> path, final String message) {
        Message m = mTrezor.io(TrezorMessage.SignMessage.newBuilder()
                               .addAllAddressN(path)
                               .setMessage(ByteString.copyFromUtf8(message)));
        m = handleCommon(parent, m);
        if (m.getClass().getSimpleName().equals("MessageSignature")) {
            final TrezorMessage.MessageSignature ms = (TrezorMessage.MessageSignature)m;
            final byte[] expanded = ms.getSignature().toByteArray();
            // Convert sig to DER encoding
            final byte[] compact = new byte[64];
            System.arraycopy(expanded, 1, compact, 0, 32);
            System.arraycopy(expanded, 33, compact, 32, 32);

            final byte[] der = new byte[Wally.EC_SIGNATURE_DER_MAX_LEN];
            final int len = Wally.ec_sig_to_der(compact, der);
            return Wally.hex_from_bytes(Arrays.copyOf(der, len));
        }
        throw new IllegalStateException("Unknown response: " + m.getClass().getSimpleName());
    }

    @Override
    public List<String> signTransaction(final GaActivity parent, final ObjectNode tx,
                                        final List<InputOutputData> inputs,
                                        final List<InputOutputData> outputs,
                                        final Map<String, String> transactions,
                                        final List<String> addressTypes)
    {
        try {
            return signTransactionImpl(parent, tx, inputs, outputs, transactions, addressTypes);
        } finally {
            // Free all wally txs to ensure we don't leak any memory
            for (Map.Entry<String, Object> entry : mPrevTxs.entrySet()) {
                Wally.tx_free(entry.getValue());
            }
            mPrevTxs.clear();
        }
    }

    @Override
    public LiquidHWResult signLiquidTransaction(GaActivity parent, ObjectNode tx, List<InputOutputData> inputs,
                                                List<InputOutputData> outputs, Map<String, String> transactions,
                                                List<String> addressTypes) {
        return null;
    }

    private List<String> signTransactionImpl(final GaActivity parent, final ObjectNode tx,
                                             final List<InputOutputData> inputs,
                                             final List<InputOutputData> outputs,
                                             final Map<String, String> transactions,
                                             final List<String> addressTypes)
    {
        final String[] signatures = new String[inputs.size()];

        final int txVersion = tx.get("transaction_version").asInt();
        final int txLocktime = tx.get("transaction_locktime").asInt();

        if (transactions != null) {
            for (Map.Entry<String, String> t : transactions.entrySet())
                mPrevTxs.put(t.getKey(), Wally.tx_from_hex(t.getValue(), Wally.WALLY_TX_FLAG_USE_WITNESS));
        }

        // Fetch and cache all required pubkeys before signing
        for (final InputOutputData in : inputs)
            makeRedeemScript(parent, in);
        for (final InputOutputData out : outputs)
            if (out.getIsChange())
                makeRedeemScript(parent, out);

        Message m = mTrezor.io(TrezorMessage.SignTx.newBuilder()
                               .setInputsCount(inputs.size())
                               .setOutputsCount(outputs.size())
                               .setCoinName(mNetwork.getMainnet() ? "Bitcoin" : "Testnet")
                               .setVersion(txVersion)
                               .setLockTime(txLocktime));
        while (true) {
            m = handleCommon(parent, m);
            switch (m.getClass().getSimpleName()) {

            case "TxRequest": {
                final TrezorMessage.TxRequest r = (TrezorMessage.TxRequest)m;

                if (r.getSerialized() != null && r.getSerialized().hasSignatureIndex())
                    signatures[r.getSerialized().getSignatureIndex()] =
                        Wally.hex_from_bytes(r.getSerialized().getSignature().toByteArray()) +"01";

                if (r.getRequestType().equals(TrezorType.RequestType.TXFINISHED))
                    return Arrays.asList(signatures);

                final TrezorType.TxRequestDetailsType txRequest = r.getDetails();
                TrezorType.TransactionType.Builder ack = TrezorType.TransactionType.newBuilder();

                if (r.getRequestType().equals(TrezorType.RequestType.TXINPUT)) {
                    m = txio(ack.addInputs(createInput(parent, txRequest, inputs)));
                    continue;
                } else if (r.getRequestType().equals(TrezorType.RequestType.TXOUTPUT)) {
                    if (txRequest.hasTxHash())
                        m = txio(ack.addBinOutputs(createBinOutput(txRequest)));
                    else
                        m = txio(ack.addOutputs(createOutput(parent, txRequest, outputs)));
                    continue;
                } else if (r.getRequestType().equals(TrezorType.RequestType.TXMETA)) {
                    if (txRequest.hasTxHash()) {
                        final Object prevTx = findPrevTx(txRequest);
                        m = txio(ack.setInputsCnt(Wally.tx_get_num_inputs(prevTx))
                                 .setOutputsCnt(Wally.tx_get_num_outputs(prevTx))
                                 .setVersion(Wally.tx_get_version(prevTx))
                                 .setLockTime(Wally.tx_get_locktime(prevTx)));
                    } else {
                        m = txio(ack.setInputsCnt(inputs.size())
                                 .setOutputsCnt(outputs.size())
                                 .setVersion(txVersion)
                                 .setLockTime(txLocktime));
                    }
                    continue;
                }
                throw new IllegalStateException("Unknown response: " + m.getClass().getSimpleName());
            }
            default:
                throw new IllegalStateException("Unknown response: " + m.getClass().getSimpleName());
            }
        }
    }

    private Message txio(final TrezorType.TransactionType.Builder ack) {
        return mTrezor.io(TrezorMessage.TxAck.newBuilder().setTx(ack));
    }

    private Object findPrevTx(final TrezorType.TxRequestDetailsType txRequest) {
        final String key = Wally.hex_from_bytes(txRequest.getTxHash().toByteArray());
        return mPrevTxs.get(key);
    }

    private TrezorType.HDNodeType getUserXpub(final GaActivity parent, final List<Integer> path) {
        final String key = Joiner.on("/").join(path);

        if (!mUserXPubs.containsKey(key)) {
            Message m = mTrezor.io(TrezorMessage.GetPublicKey.newBuilder().addAllAddressN(path));
            m = handleCommon(parent, m);
            if (m.getClass().getSimpleName().equals("PublicKey")) {
                final TrezorMessage.PublicKey pk = (TrezorMessage.PublicKey)m;
                mUserXPubs.put(key,
                               TrezorType.HDNodeType.newBuilder()
                               .setDepth(1)
                               .setFingerprint(0)
                               .setChildNum(path.isEmpty() ? 0 : path.get(path.size() -1))
                               .setPublicKey(pk.getNode().getPublicKey())
                               .setChainCode(pk.getNode().getChainCode()).build());
            } else {
                throw new IllegalStateException("Unknown response: " + m.getClass().getSimpleName());
            }
        }
        return mUserXPubs.get(key);
    }

    private TrezorType.HDNodeType getXpub(final Map<String, TrezorType.HDNodeType> cache, final String xpub58) {
        if (!cache.containsKey(xpub58)) {
            final Object k = Wally.bip32_key_from_base58(xpub58);
            cache.put(xpub58, TrezorType.HDNodeType.newBuilder()
                      .setDepth(Wally.bip32_key_get_depth(k))
                      .setFingerprint(0).setChildNum(Wally.bip32_key_get_child_num(k))
                      .setPublicKey(ByteString.copyFrom(Wally.bip32_key_get_pub_key(k)))
                      .setChainCode(ByteString.copyFrom(Wally.bip32_key_get_chain_code(k))).build());
            Wally.bip32_key_free(k);
        }
        return cache.get(xpub58);
    }

    @Override
    public String getBlindingKey(GaActivity parent, String scriptHex) {
        return null;
    }

    @Override
    public String getBlindingNonce(GaActivity parent, String pubkey, String scriptHex) {
        return null;
    }

    private TrezorType.HDNodePathType makeHDNode(final TrezorType.HDNodeType node, final Integer pointer) {
        return TrezorType.HDNodePathType.newBuilder().setNode(node).addAddressN(pointer).build();
    }

    private TrezorType.MultisigRedeemScriptType makeRedeemScript(final GaActivity parent, final InputOutputData in) {
        final int pointer = in.getPointer();
        final TrezorType.HDNodeType serviceParent = getXpub(mServiceXPubs, in.getServiceXpub());
        final TrezorType.HDNodeType userParent =
            getUserXpub(parent, in.getUserPathAsInts().subList(0, in.getUserPath().size() - 1));

        TrezorType.MultisigRedeemScriptType.Builder b = TrezorType.MultisigRedeemScriptType.newBuilder()
                                                        .addPubkeys(makeHDNode(serviceParent, pointer))
                                                        .addPubkeys(makeHDNode(userParent, pointer));
        if (in.getRecoveryXpub() != null) {
            // 2of3
            final TrezorType.HDNodeType recoveryParent = getXpub(mRecoveryXPubs, in.getRecoveryXpub());
            b.addPubkeys(makeHDNode(recoveryParent, pointer));
        }
        return b.setM(2).build();
    }

    private TrezorType.TxOutputType.Builder createOutput(final GaActivity parent,
                                                         final TrezorType.TxRequestDetailsType txRequest,
                                                         final List<InputOutputData> outputs) {
        final InputOutputData out = outputs.get(txRequest.getRequestIndex());
        final TrezorType.TxOutputType.Builder b = TrezorType.TxOutputType.newBuilder().setAmount(out.getSatoshi());

        if (out.getIsChange()) {
            b.setScriptType(out.getAddressType().equals("p2sh") ?
                            TrezorType.OutputScriptType.PAYTOMULTISIG : TrezorType.OutputScriptType.PAYTOP2SHWITNESS);
            return b.addAllAddressN(out.getUserPathAsInts()).setMultisig(makeRedeemScript(parent, out));
        } else {
            return b.setAddress(out.getAddress()).setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS);
        }
    }

    private TrezorType.TxOutputBinType.Builder createBinOutput(final TrezorType.TxRequestDetailsType txRequest) {
        final Object prevTx = findPrevTx(txRequest);
        return TrezorType.TxOutputBinType.newBuilder()
               .setAmount(Wally.tx_get_output_satoshi(prevTx, txRequest.getRequestIndex()))
               .setScriptPubkey(ByteString.copyFrom(Wally.tx_get_output_script(prevTx, txRequest.getRequestIndex())));
    }

    private TrezorType.TxInputType.Builder createInput(final GaActivity parent,
                                                       final TrezorType.TxRequestDetailsType txRequest,
                                                       final List<InputOutputData> inputs) {
        final int index = txRequest.getRequestIndex();

        final boolean isPrevTx = txRequest.hasTxHash();
        if (isPrevTx) {
            final Object prevTx = findPrevTx(txRequest);
            final byte[] txhash = InputOutputData.reverseBytes(Wally.tx_get_input_txhash(prevTx, index));
            return TrezorType.TxInputType.newBuilder()
                   .setPrevHash(ByteString.copyFrom(txhash))
                   .setPrevIndex(Wally.tx_get_input_index(prevTx, index))
                   .setSequence((int) Wally.tx_get_input_sequence(prevTx, index))
                   .setScriptSig(ByteString.copyFrom(Wally.tx_get_input_script(prevTx, index)));
        }

        final InputOutputData in = inputs.get(index);
        TrezorType.TxInputType.Builder txin;
        txin = TrezorType.TxInputType.newBuilder()
               .setPrevHash(ByteString.copyFrom(Wally.hex_to_bytes(in.getTxhash())))
               .setPrevIndex((int) (long) in.getPtIdx())
               .setSequence((int) (long) in.getSequence())
               .addAllAddressN(in.getUserPathAsInts())
               .setMultisig(makeRedeemScript(parent, in));

        if (in.getScriptType() == 14 || in.getScriptType() == 15)
            return txin.setScriptType(TrezorType.InputScriptType.SPENDP2SHWITNESS)
                   .setAmount(in.getSatoshi());
        return txin.setScriptType(TrezorType.InputScriptType.SPENDMULTISIG);
    }

    private Message handleCommon(final GaActivity parent, final Message m) {
        switch (m.getClass().getSimpleName()) {
        case "ButtonRequest":
            parent.interactionRequest(this);
            return handleCommon(parent, mTrezor.io(TrezorMessage.ButtonAck.newBuilder()));

        case "PinMatrixRequest":
            final String pin = parent.pinMatrixRequest(this);
            return handleCommon(parent, mTrezor.io(TrezorMessage.PinMatrixAck.newBuilder().setPin(pin)));

        case "PassphraseStateRequest":
            return handleCommon(parent, mTrezor.io(TrezorMessage.PassphraseStateAck.newBuilder()));

        case "PassphraseRequest":
            TrezorMessage.PassphraseRequest passphraseRequest = (TrezorMessage.PassphraseRequest)m;
            TrezorMessage.PassphraseAck.Builder ackBuilder = TrezorMessage.PassphraseAck.newBuilder();

            // on the Trezor One hasOnDevice is false (you can't possibly enter the password there)
            // on the Trezor T hasOnDevice is true, so we check what it is explicitly asking with getOnDevice
            if (!passphraseRequest.hasOnDevice() || !passphraseRequest.getOnDevice()) {
                // Passphrase set to "HOST", ask the user here on the app
                final String passphrase = parent.passphraseRequest(this);
                ackBuilder.setPassphrase(passphrase);
            }

            return handleCommon(parent, mTrezor.io(ackBuilder));

        case "Failure":
            final String message = ((TrezorMessage.Failure)m).getMessage();
            Log.e(TAG, "Trezor Failure response: " + message);
            throw new IllegalStateException(message);

        default:
            return m;
        }
    }

    public int getIconResourceId() {
        return R.drawable.ic_trezor;
    }

    @Override
    public String getGreenAddress(final boolean csv, final long subaccount, final long branch, final long pointer,
                                  final long csvBlocks) {
        return null;
    }
}
