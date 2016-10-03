package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    public final static String GAIT_WAMP_URL = "wss://alphawss.greenaddress.it/ws/inv";
    public final static String[] GAIT_WAMP_CERT_PINS = {
        "C9:01:C4:05:AE:44:A6:29:33:B7:E2:66:26:F6:2E:1A:FC:CF:B9:E6:C0:8F:D0:44:B0:94:85:35:17:39:F5:79"
    };
    public final static String BLOCKEXPLORER_ADDRESS = "https://test-insight.bitpay.com/address/";
    public final static String BLOCKEXPLORER_TX = "https://test-insight.bitpay.com/tx/";
    public final static String depositPubkey = "036307e560072ed6ce0aa5465534fb5c258a2ccfbc257f369e8e7a181b16d897b3";
    public final static String depositChainCode = "b60befcc619bb1c212732770fe181f2f1aa824ab89f8aab49f2e13e3a56f0f04";
    public final static String GAIT_ONION = null;
    public final static String DEFAULT_PEER = "";
}
