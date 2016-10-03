package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    public final static String GAIT_WAMP_URL = "wss://testwss.greenaddress.it/v2/ws/";
    public final static String[] GAIT_WAMP_CERT_PINS = {
        "C9:01:C4:05:AE:44:A6:29:33:B7:E2:66:26:F6:2E:1A:FC:CF:B9:E6:C0:8F:D0:44:B0:94:85:35:17:39:F5:79",
        "8E:36:FA:1F:D4:3F:49:22:12:7C:13:0D:7C:36:79:06:46:86:3B:E8:7A:4A:6A:4C:A5:0E:58:D2:A5:EA:85:E0"
    };
    public final static String BLOCKEXPLORER_ADDRESS = "https://sandbox.smartbit.com.au/address/";
    public final static String BLOCKEXPLORER_TX = "https://sandbox.smartbit.com.au/tx/";
    public final static String depositPubkey = "036307e560072ed6ce0aa5465534fb5c258a2ccfbc257f369e8e7a181b16d897b3";
    public final static String depositChainCode = "b60befcc619bb1c212732770fe181f2f1aa824ab89f8aab49f2e13e3a56f0f04";
    public final static String GAIT_ONION = "gu5ke7a2aguwfqhz.onion";
    public final static String DEFAULT_PEER = "";
}
