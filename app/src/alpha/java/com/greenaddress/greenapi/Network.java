package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    public final static String GAIT_WAMP_URL = "wss://alphawss.greenaddress.it/ws/inv";
    public final static String[] GAIT_WAMP_CERT_PINS = {
        "94:3C:50:EF:B5:31:BD:6B:8D:61:77:C0:0C:B8:F2:AA:40:1C:04:1D:96:DC:37:51:DA:93:13:66:1A:DD:F6:49"
    };
    public final static String BLOCKEXPLORER_ADDRESS = "https://test-insight.bitpay.com/address/";
    public final static String BLOCKEXPLORER_TX = "https://test-insight.bitpay.com/tx/";
    public final static String depositPubkey = "036307e560072ed6ce0aa5465534fb5c258a2ccfbc257f369e8e7a181b16d897b3";
    public final static String depositChainCode = "b60befcc619bb1c212732770fe181f2f1aa824ab89f8aab49f2e13e3a56f0f04";
    public final static String GAIT_ONION = null;
    public final static String DEFAULT_PEER = "";
}
