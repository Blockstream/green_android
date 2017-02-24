package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    public final static String GAIT_WAMP_URL = "wss://testwss.greenaddress.it/v2/ws/";
    public final static String[] GAIT_WAMP_CERT_PINS = {
        // Let’s Encrypt Authority X3:
        "25:84:7D:66:8E:B4:F0:4F:DD:40:B1:2B:6B:07:40:C5:67:DA:7D:02:43:08:EB:6C:2C:96:FE:41:D9:DE:21:8D",
        // Let’s Encrypt Authority X4: (backup)
        "A7:4B:0C:32:B6:5B:95:FE:2C:4F:8F:09:89:47:A6:8B:69:50:33:BE:D0:B5:1D:D8:B9:84:EC:AE:89:57:1B:B6"
    };
    public final static String BLOCKEXPLORER_ADDRESS = "https://sandbox.smartbit.com.au/address/";
    public final static String BLOCKEXPLORER_TX = "https://sandbox.smartbit.com.au/tx/";
    public final static String depositPubkey = "036307e560072ed6ce0aa5465534fb5c258a2ccfbc257f369e8e7a181b16d897b3";
    public final static String depositChainCode = "b60befcc619bb1c212732770fe181f2f1aa824ab89f8aab49f2e13e3a56f0f04";
    public final static String GAIT_ONION = "gu5ke7a2aguwfqhz.onion";
    public final static String DEFAULT_PEER = "";
}
