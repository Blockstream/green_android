package com.greenaddress.greenapi;

import com.btchip.BTChipException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.InputOutputData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.ui.GaActivity;

import java.util.List;
import java.util.Map;

public abstract class HWWallet {
    protected NetworkData mNetwork;
    protected HWDeviceData mHWDeviceData;

    // For any explicit disconnection the hw may want to do
    public abstract void disconnect();

    // Return the base58check encoded xpubs for each path in paths
    public abstract List<String> getXpubs(final GaActivity parent, final List<List<Integer>> paths);

    // Sign message with the key resulting from path, and return it as hex encoded DER
    public abstract String signMessage(final GaActivity parent, final List<Integer> path, final String message);

    public abstract List<String> signTransaction(final GaActivity parent, final ObjectNode tx,
                                                 final List<InputOutputData> inputs,
                                                 final List<InputOutputData> outputs,
                                                 final Map<String,String> transactions,
                                                 final List<String> addressTypes);

    public static class LiquidHWResult {
        private final List<String> signatures;
        private final List<String> assetCommitments;
        private final List<String> valueCommitments;
        private final List<String> assetBlinders;
        private final List<String> amountBlinders;

        public LiquidHWResult(List<String> signatures, List<String> assetCommitments, List<String> valueCommitments, List<String> assetBlinders, List<String> amountBlinders) {
            this.signatures = signatures;
            this.assetCommitments = assetCommitments;
            this.valueCommitments = valueCommitments;
            this.assetBlinders = assetBlinders;
            this.amountBlinders = amountBlinders;
        }

        public List<String> getSignatures() {
            return signatures;
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

    public abstract LiquidHWResult signLiquidTransaction(final GaActivity parent, final ObjectNode tx,
                                                 final List<InputOutputData> inputs,
                                                 final List<InputOutputData> outputs,
                                                 final Map<String,String> transactions,
                                                 final List<String> addressTypes);

    public abstract String getBlindingKey(final GaActivity parent, final String scriptHex);

    public abstract String getBlindingNonce(final GaActivity parent, final String pubkey, final String scriptHex);

    public abstract int getIconResourceId();

    public abstract String getGreenAddress(final SubaccountData subaccount, final long branch, final long pointer, final long csvBlocks) throws Exception;

    public NetworkData getNetwork() {
        return mNetwork;
    }

    public HWDeviceData getHWDeviceData() {
        return mHWDeviceData;
    }
}
