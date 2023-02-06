package com.greenaddress.greenapi;

import com.blockstream.gdk.data.Account;
import com.blockstream.gdk.data.Device;
import com.blockstream.gdk.data.InputOutput;
import com.blockstream.gdk.data.Network;
import com.blockstream.gdk.data.SubAccount;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public abstract class HWWallet {
    protected Device mDevice;
    protected HardwareQATester mHardwareQATester;
    protected String mFirmwareVersion;
    protected String mModel;

    public static class SignMsgResult {
        private final String signature;
        private final String signerCommitment;

        public SignMsgResult(final String signature, final String signerCommitment) {
            this.signature = signature;
            this.signerCommitment = signerCommitment;
        }

        public String getSignature() {
            return signature;
        }

        public String getSignerCommitment() {
            return signerCommitment;
        }
    }

    public static class SignTxResult {
        private final List<String> signatures;
        private final List<String> signerCommitments;
        private final List<String> assetCommitments;
        private final List<String> valueCommitments;
        private final List<String> assetBlinders;
        private final List<String> amountBlinders;

        // Ctor for standard btc signature results
        public SignTxResult(List<String> signatures, List<String> signerCommitments) {
            this.signatures = signatures;
            this.signerCommitments = signerCommitments;
            this.assetCommitments = null;
            this.valueCommitments = null;
            this.assetBlinders = null;
            this.amountBlinders = null;
        }

        // Ctor for liquid signature results
        public SignTxResult(List<String> signatures, List<String> signerCommitments, List<String> assetCommitments, List<String> valueCommitments, List<String> assetBlinders, List<String> amountBlinders) {
            this.signatures = signatures;
            this.signerCommitments = signerCommitments;
            this.assetCommitments = assetCommitments;
            this.valueCommitments = valueCommitments;
            this.assetBlinders = assetBlinders;
            this.amountBlinders = amountBlinders;
        }

        public List<String> getSignatures() {
            return signatures;
        }

        public List<String> getSignerCommitments() {
            return signerCommitments;
        }

        public List<String> getAssetCommitments() {
            return assetCommitments;
        }

        public List<String> getValueCommitments() {
            return valueCommitments;
        }

        public List<String> getAssetBlinders() {
            return assetBlinders;
        }

        public List<String> getAmountBlinders() {
            return amountBlinders;
        }
    }

    // For any explicit disconnection the hw may want to do
    public abstract void disconnect();

    // Return the base58check encoded xpubs for each path in paths
    public abstract List<String> getXpubs(final Network network, final HWWalletBridge parent, final List<List<Integer>> paths);

    // Sign message with the key resulting from path, and return it as hex encoded DER
    // If using Anti-Exfil protocol, also return the signerCommitment (if not this can be null).
    public abstract SignMsgResult signMessage(final HWWalletBridge parent, final List<Integer> path, final String message,
                                              final boolean useAeProtocol, final String aeHostCommitment, final String aeHostEntropy);

    public abstract SignTxResult signTransaction(final Network network, final HWWalletBridge parent, final ObjectNode tx,
                                                 final List<InputOutput> inputs,
                                                 final List<InputOutput> outputs,
                                                 final Map<String,String> transactions,
                                                 final boolean useAeProtocol);

    public abstract SignTxResult signLiquidTransaction(final Network network, final HWWalletBridge parent, final ObjectNode tx,
                                                       final List<InputOutput> inputs,
                                                       final List<InputOutput> outputs,
                                                       final Map<String,String> transactions,
                                                       final boolean useAeProtocol);

    public abstract String getMasterBlindingKey(final HWWalletBridge parent);

    public abstract String getBlindingKey(final HWWalletBridge parent, final String scriptHex);

    public abstract String getBlindingNonce(final HWWalletBridge parent, final String pubkey, final String scriptHex);

    public abstract int getIconResourceId();

    public abstract String getGreenAddress(final Network network, HWWalletBridge parent, final Account account, final List<Long> path, final long csvBlocks) throws Exception;

    public Device getDevice() {
        return mDevice;
    }

    @Nullable
    public PublishSubject<Boolean> getBleDisconnectEvent(){
        return null;
    }

    @Nullable
    public HardwareQATester getHardwareEmulator() {
        return mHardwareQATester;
    }

    @Nullable
    public String getFirmwareVersion() {
        return mFirmwareVersion;
    }

    public String getModel(){
        return mModel;
    }
}
