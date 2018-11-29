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
                                                 final List<String> adressTypes);

    public abstract int getIconResourceId();
}
