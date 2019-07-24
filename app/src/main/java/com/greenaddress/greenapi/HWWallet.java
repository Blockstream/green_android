package com.greenaddress.greenapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.InputOutputData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.GaActivity;

import java.util.List;
import java.util.Map;

public abstract class HWWallet {
    protected NetworkData mNetwork;

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
        private final List<String> abfs;
        private final List<String> vbfs;

        public LiquidHWResult(List<String> signatures, List<String> assetCommitments, List<String> valueCommitments, List<String> abfs, List<String> vbfs) {
            this.signatures = signatures;
            this.assetCommitments = assetCommitments;
            this.valueCommitments = valueCommitments;
            this.abfs = abfs;
            this.vbfs = vbfs;
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

        public List<String> getAbfs() {
            return abfs;
        }

        public List<String> getVbfs() {
            return vbfs;
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

    public NetworkData getNetwork() {
        return mNetwork;
    }
}
