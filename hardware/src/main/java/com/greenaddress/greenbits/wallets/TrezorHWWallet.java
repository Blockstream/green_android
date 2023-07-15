package com.greenaddress.greenbits.wallets;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blockstream.common.extensions.GdkExtensionsKt;
import com.blockstream.common.gdk.Gdk;
import com.blockstream.common.gdk.data.Account;
import com.blockstream.common.gdk.data.AccountType;
import com.blockstream.common.gdk.data.Device;
import com.blockstream.common.gdk.data.InputOutput;
import com.blockstream.common.gdk.data.Network;
import com.blockstream.common.gdk.device.BlindingFactorsResult;
import com.blockstream.common.gdk.device.DeviceBrand;
import com.blockstream.common.gdk.device.HardwareWalletInteraction;
import com.blockstream.common.gdk.device.SignMessageResult;
import com.blockstream.common.gdk.device.SignTransactionResult;
import com.blockstream.libwally.Wally;
import com.google.common.base.Joiner;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.greenaddress.greenapi.HWWallet;
import com.satoshilabs.trezor.Trezor;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlinx.coroutines.CompletableDeferred;
import kotlinx.coroutines.CompletableDeferredKt;
import kotlinx.coroutines.flow.MutableStateFlow;


public class TrezorHWWallet extends HWWallet {

    private static final String TAG = TrezorHWWallet.class.getSimpleName();

    private final Trezor mTrezor;
    private final Map<String, TrezorType.HDNodeType> mUserXPubs = new HashMap<>();
    private final Map<String, TrezorType.HDNodeType> mServiceXPubs = new HashMap<>();
    private final Map<String, TrezorType.HDNodeType> mRecoveryXPubs = new HashMap<>();
    private final Map<String, Object> mPrevTxs = new HashMap<>();

    public TrezorHWWallet(final Gdk gdk, final Trezor t, final Device device, final String firmwareVersion) {
        mGdk = gdk;
        mTrezor = t;
        mDevice = device;
        mFirmwareVersion = firmwareVersion;
        mModel = "Trezor " + mTrezor.getModel();
    }

    @Override
    public synchronized void disconnect() {
        // No-op
    }

    @NonNull
    @Override
    public synchronized List<String> getXpubs(@NonNull Network network, @Nullable HardwareWalletInteraction hwInteraction, @NonNull List<? extends List<Integer>> paths) {
        final List<String> xpubs = new ArrayList<>(paths.size());

        for (List<Integer> path : paths) {
            final TrezorType.HDNodeType xpub = getUserXpub(hwInteraction, path);

            final Object hdkey = Wally.bip32_pub_key_init(network.getVerPublic(),
                                                          1, 1, xpub.getChainCode().toByteArray(),
                                                          xpub.getPublicKey().toByteArray());

            xpubs.add(Wally.bip32_key_to_base58(hdkey, Wally.BIP32_FLAG_KEY_PUBLIC));
            Wally.bip32_key_free(hdkey);
        }
        return xpubs;
    }

    @NonNull
    @Override
    public SignMessageResult signMessage(@Nullable HardwareWalletInteraction hwInteraction, @NonNull List<Integer> path, @NonNull String message, boolean useAeProtocol, @Nullable String aeHostCommitment, @Nullable String aeHostEntropy) {
        if (useAeProtocol) {
            throw new RuntimeException("Hardware Wallet does not support the Anti-Exfil protocol");
        }

        TrezorMessage.SignMessage.Builder builder = TrezorMessage.SignMessage.newBuilder()
                .addAllAddressN(path)
                .setMessage(ByteString.copyFromUtf8(message));

        if(path.size() > 0){
            TrezorType.InputScriptType scriptType = mapAccountType(path.get(0));
            if(scriptType != null){
                builder.setScriptType(scriptType);
            }
        }

        Message m = mTrezor.io(builder);

        m = handleCommon(hwInteraction, m);
        if (m.getClass().getSimpleName().equals("MessageSignature")) {
            final TrezorMessage.MessageSignature ms = (TrezorMessage.MessageSignature)m;
            final byte[] expanded = ms.getSignature().toByteArray();
            // Convert sig to DER encoding
            final byte[] compact = new byte[64];
            System.arraycopy(expanded, 1, compact, 0, 32);
            System.arraycopy(expanded, 33, compact, 32, 32);

            final byte[] der = new byte[Wally.EC_SIGNATURE_DER_MAX_LEN];
            final int len = Wally.ec_sig_to_der(compact, der);
            final String signature = Wally.hex_from_bytes(Arrays.copyOf(der, len));
            return new SignMessageResult(signature, null);
        }
        throw new IllegalStateException("Unknown response: " + m.getClass().getSimpleName());
    }

    @NonNull
    @Override
    public SignTransactionResult signTransaction(@NonNull Network network, @Nullable HardwareWalletInteraction hwInteraction, @NonNull String transaction, @NonNull List<InputOutput> inputs, @NonNull List<InputOutput> outputs, @Nullable Map<String, String> transactions, boolean useAeProtocol) {
        if(network.isLiquid()){
            throw new RuntimeException(network.getCanonicalName() + " is not supported");
        }
        try {

            if (useAeProtocol) {
                throw new RuntimeException("Hardware Wallet does not support the Anti-Exfil protocol");
            }

            return signTransactionImpl(network, hwInteraction, transaction, inputs, outputs, transactions);
        } finally {
            // Free all wally txs to ensure we don't leak any memory
            for (Map.Entry<String, Object> entry : mPrevTxs.entrySet()) {
                Wally.tx_free(entry.getValue());
            }
            mPrevTxs.clear();
        }
    }

    private synchronized SignTransactionResult signTransactionImpl(final Network network, @Nullable HardwareWalletInteraction hwInteraction, final String transaction,
                                             final List<InputOutput> inputs,
                                             final List<InputOutput> outputs,
                                             final Map<String, String> transactions)
    {
        final String[] signatures = new String[inputs.size()];

        final byte[] txBytes = Wally.hex_to_bytes(transaction);
        final Object wallytx = Wally.tx_from_bytes(txBytes, Wally.WALLY_TX_FLAG_USE_WITNESS);

        final int txVersion = Wally.tx_get_version(wallytx);
        final int txLocktime = Wally.tx_get_locktime(wallytx);

        if (transactions != null) {
            for (Map.Entry<String, String> t : transactions.entrySet())
                mPrevTxs.put(t.getKey(), Wally.tx_from_hex(t.getValue(), Wally.WALLY_TX_FLAG_USE_WITNESS));
        }

        // Fetch and cache all required pubkeys before signing
        if (network.isMultisig()) {
            for (final InputOutput in : inputs)
                makeMultisigRedeemScript(hwInteraction, in);
            for (final InputOutput out : outputs)
                if (out.isChange() != null && out.isChange())
                    makeMultisigRedeemScript(hwInteraction, out);
        }

        Message m = mTrezor.io(TrezorMessage.SignTx.newBuilder()
                               .setInputsCount(inputs.size())
                               .setOutputsCount(outputs.size())
                               .setCoinName(network.isMainnet() ? "Bitcoin" : "Testnet")
                               .setVersion(txVersion)
                               .setLockTime(txLocktime));
        while (true) {
            m = handleCommon(hwInteraction, m);
            switch (m.getClass().getSimpleName()) {

            case "TxRequest": {
                final TrezorMessage.TxRequest r = (TrezorMessage.TxRequest)m;

                if (r.getSerialized() != null && r.getSerialized().hasSignatureIndex())
                    signatures[r.getSerialized().getSignatureIndex()] =
                        Wally.hex_from_bytes(r.getSerialized().getSignature().toByteArray()) +"01";

                if (r.getRequestType().equals(TrezorType.RequestType.TXFINISHED))
                    return new SignTransactionResult(Arrays.asList(signatures), null);

                final TrezorType.TxRequestDetailsType txRequest = r.getDetails();
                TrezorType.TransactionType.Builder ack = TrezorType.TransactionType.newBuilder();

                if (r.getRequestType().equals(TrezorType.RequestType.TXINPUT)) {
                    m = txio(ack.addInputs(createInput(network, hwInteraction, txRequest, inputs)));
                    continue;
                } else if (r.getRequestType().equals(TrezorType.RequestType.TXOUTPUT)) {
                    if (txRequest.hasTxHash())
                        m = txio(ack.addBinOutputs(createBinOutput(txRequest)));
                    else
                        m = txio(ack.addOutputs(createOutput(network, hwInteraction, txRequest, outputs)));
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

    private TrezorType.HDNodeType getUserXpub(@Nullable HardwareWalletInteraction hwInteraction, final List<Integer> path) {
        final String key = Joiner.on("/").join(path);

        if (!mUserXPubs.containsKey(key)) {
            Message m = mTrezor.io(TrezorMessage.GetPublicKey.newBuilder().addAllAddressN(path));
            m = handleCommon(hwInteraction, m);
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

    @NonNull
    @Override
    public synchronized String getMasterBlindingKey(@Nullable HardwareWalletInteraction hwInteraction) {
        throw new RuntimeException("Master Blinding Key is not supported");
    }

    @Override
    public synchronized String getBlindingKey(@Nullable HardwareWalletInteraction hwInteraction, String scriptHex) {
        throw new RuntimeException("Master Blinding Key is not supported");
    }

    @Override
    public synchronized String getBlindingNonce(@Nullable HardwareWalletInteraction hwInteraction, String pubkey, String scriptHex) {
        throw new RuntimeException("Master Blinding Key is not supported");
    }

    @Override
    public synchronized BlindingFactorsResult getBlindingFactors(@Nullable HardwareWalletInteraction hwInteraction, final List<InputOutput> inputs, final List<InputOutput> outputs) {
        throw new RuntimeException("Master Blinding Key is not supported");
    }

    private TrezorType.HDNodePathType makeHDNode(final TrezorType.HDNodeType node, final Integer pointer) {
        return TrezorType.HDNodePathType.newBuilder().setNode(node).addAddressN(pointer).build();
    }

    private TrezorType.MultisigRedeemScriptType makeMultisigRedeemScript(@Nullable HardwareWalletInteraction hwInteraction, final InputOutput in) {
        final int pointer = in.getPointer();
        final TrezorType.HDNodeType serviceParent = getXpub(mServiceXPubs, in.getServiceXpub());
        final TrezorType.HDNodeType userParent =
                getUserXpub(hwInteraction, in.getUserPathAsInts().subList(0, in.getUserPath().size() - 1));

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

    private TrezorType.TxOutputType.Builder createOutput(final Network network, @Nullable HardwareWalletInteraction hwInteraction,
                                                         final TrezorType.TxRequestDetailsType txRequest,
                                                         final List<InputOutput> outputs) {
        final InputOutput out = outputs.get(txRequest.getRequestIndex());
        final TrezorType.TxOutputType.Builder b = TrezorType.TxOutputType.newBuilder();

        b.setAmount(out.getSatoshi());
        if (out.getAddressType() != null) {
            switch (out.getAddressType()) {
                case "p2sh":
                    b.setScriptType(TrezorType.OutputScriptType.PAYTOSCRIPTHASH);
                    break;
                case "p2pkh":
                    b.setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS);
                    break;
                case "p2wpkh":
                    b.setScriptType(TrezorType.OutputScriptType.PAYTOWITNESS);
                    break;
                default:  // csv ?
                    b.setScriptType(TrezorType.OutputScriptType.PAYTOP2SHWITNESS);
            }
        } else {
            // Don't know address type ?
            b.setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS);
        }

        if (out.isChange() != null && out.isChange()) {
            // Change address - set path elements
            b.addAllAddressN(out.getUserPathAsInts());
            if (network.isMultisig()) {
                // Green Multisig Shield
                b.setMultisig(makeMultisigRedeemScript(hwInteraction, out));
            }
        } else {
            // Not a change output - just set address
            b.setAddress(out.getAddress());
        }
        return b;
    }

    private TrezorType.TxOutputBinType.Builder createBinOutput(final TrezorType.TxRequestDetailsType txRequest) {
        final Object prevTx = findPrevTx(txRequest);
        return TrezorType.TxOutputBinType.newBuilder()
               .setAmount(Wally.tx_get_output_satoshi(prevTx, txRequest.getRequestIndex()))
               .setScriptPubkey(ByteString.copyFrom(Wally.tx_get_output_script(prevTx, txRequest.getRequestIndex())));
    }

    private TrezorType.TxInputType.Builder createInput(final Network network, @Nullable HardwareWalletInteraction hwInteraction,
                                                       final TrezorType.TxRequestDetailsType txRequest,
                                                       final List<InputOutput> inputs) {
        final int index = txRequest.getRequestIndex();

        final boolean isPrevTx = txRequest.hasTxHash();
        if (isPrevTx) {
            final Object prevTx = findPrevTx(txRequest);
            final byte[] txhash = GdkExtensionsKt.reverseBytes(Wally.tx_get_input_txhash(prevTx, index));
            return TrezorType.TxInputType.newBuilder()
                   .setPrevHash(ByteString.copyFrom(txhash))
                   .setPrevIndex(Wally.tx_get_input_index(prevTx, index))
                   .setSequence((int) Wally.tx_get_input_sequence(prevTx, index))
                   .setScriptSig(ByteString.copyFrom(Wally.tx_get_input_script(prevTx, index)));
        }

        final InputOutput in = inputs.get(index);
        TrezorType.TxInputType.Builder txin;
        txin = TrezorType.TxInputType.newBuilder()
               .setPrevHash(ByteString.copyFrom(Wally.hex_to_bytes(in.getTxHash())))
               .setPrevIndex(in.getPtIdxInt())
               .setSequence(in.getSequenceInt())
               .addAllAddressN(in.getUserPathAsInts());

        if (network.isMultisig()) {
            txin.setMultisig(makeMultisigRedeemScript(hwInteraction, in));
        }

        switch (in.getAddressType()) {
            case "p2sh":
                return txin.setScriptType(TrezorType.InputScriptType.SPENDMULTISIG);
            case "p2pkh":
                return txin.setScriptType(TrezorType.InputScriptType.SPENDADDRESS);
            case "p2wpkh":
                return txin.setScriptType(TrezorType.InputScriptType.SPENDWITNESS)
                        .setAmount(in.getSatoshi());
            default:
                return txin.setScriptType(TrezorType.InputScriptType.SPENDP2SHWITNESS)
                        .setAmount(in.getSatoshi());
        }
    }

    private Message handleCommon(final HardwareWalletInteraction hwInteraction, final Message m) {
        switch (m.getClass().getSimpleName()) {
        case "ButtonRequest":
            CompletableDeferred completable = CompletableDeferredKt.CompletableDeferred(null);
            if(hwInteraction != null) {
                hwInteraction.interactionRequest(this, completable, "id_check_your_device");
            }
            Message io = mTrezor.io(TrezorMessage.ButtonAck.newBuilder());
            completable.complete(true);
            return handleCommon(hwInteraction, io);

        case "PinMatrixRequest":
            final String pin = hwInteraction.requestPinMatrix(DeviceBrand.Trezor);
            return handleCommon(hwInteraction, mTrezor.io(TrezorMessage.PinMatrixAck.newBuilder().setPin(pin)));

        case "PassphraseStateRequest":
            return handleCommon(hwInteraction, mTrezor.io(TrezorMessage.PassphraseStateAck.newBuilder()));

        case "PassphraseRequest":
            TrezorMessage.PassphraseRequest passphraseRequest = (TrezorMessage.PassphraseRequest)m;
            TrezorMessage.PassphraseAck.Builder ackBuilder = TrezorMessage.PassphraseAck.newBuilder();

            // on the Trezor One hasOnDevice is false (you can't possibly enter the password there)
            // on the Trezor T hasOnDevice is true, so we check what it is explicitly asking with getOnDevice
            if (!passphraseRequest.hasOnDevice() || !passphraseRequest.getOnDevice()) {
                // Passphrase set to "HOST", ask the user here on the app
                final String passphrase = hwInteraction.requestPassphrase(DeviceBrand.Trezor);
                ackBuilder.setPassphrase(passphrase);
            }

            return handleCommon(hwInteraction, mTrezor.io(ackBuilder));

        case "Failure":
            final String message = ((TrezorMessage.Failure)m).getMessage();
            Log.e(TAG, "Trezor Failure response: " + message);
            throw new IllegalStateException(message);

        default:
            return m;
        }
    }

    private static List<Integer> getIntegerPath(final List<Long> unsigned) {
        //return unsigned.stream().map(Long::intValue).collect(Collectors.toList());
        final List<Integer> signed = new ArrayList<>(unsigned.size());
        for (final Long n : unsigned) {
            signed.add(n.intValue());
        }
        return signed;
    }

    private static TrezorType.InputScriptType mapAccountType(final AccountType accountType) {
        switch (accountType) {
            case STANDARD:
            case AMP_ACCOUNT:
            case TWO_OF_THREE:
                return TrezorType.InputScriptType.SPENDMULTISIG;
            case BIP44_LEGACY:
                return TrezorType.InputScriptType.SPENDADDRESS;
            case BIP84_SEGWIT:
                return TrezorType.InputScriptType.SPENDWITNESS;
            case BIP49_SEGWIT_WRAPPED:
                return TrezorType.InputScriptType.SPENDP2SHWITNESS;
            default:
                return null;
        }
    }

    private static Integer unharden(final Integer i) {
        return Integer.MIN_VALUE + i;
    }

    private static TrezorType.InputScriptType mapAccountType(final Integer path) {
        return switch (unharden(path)) {
            case 44 -> TrezorType.InputScriptType.SPENDADDRESS;
            case 49 -> TrezorType.InputScriptType.SPENDP2SHWITNESS;
            case 84 -> TrezorType.InputScriptType.SPENDWITNESS;
            default -> null;
        };
    }

    @Override
    public synchronized String getGreenAddress(final Network network, HardwareWalletInteraction hwInteraction, final Account account, final List<Long> path, final long csvBlocks) {

        if (network.isMultisig()) {
            throw new RuntimeException("Hardware Wallet does not support displaying Green Multisig Shield addresses");
        }

        if (network.isLiquid()) {
            throw new RuntimeException("Hardware Wallet does not support displaying Liquid addresses");
        }

        Message m = mTrezor.io(TrezorMessage.GetAddress.newBuilder()
                        .setShowDisplay(true)
                        .setCoinName(network.isTestnet() ? "Testnet" : "Bitcoin")
                        .setScriptType(mapAccountType(account.getType()))
                        .addAllAddressN(getIntegerPath(path)));

        m = handleCommon(hwInteraction, m);
        if (m.getClass().getSimpleName().equals("Address")) {
            final TrezorMessage.Address addr = (TrezorMessage.Address)m;
            return addr.getAddress();
        }
        throw new IllegalStateException("Unknown response: " + m.getClass().getSimpleName());
    }

    @Nullable
    @Override
    public MutableStateFlow getDisconnectEvent() {
        return null;
    }
}
