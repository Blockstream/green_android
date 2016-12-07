package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    public final static String GAIT_WAMP_URL = "wss://testwss.greenaddress.it/v2/ws/";
    public final static String[] GAIT_WAMP_CERT_PINS = {
        "8E:36:FA:1F:D4:3F:49:22:12:7C:13:0D:7C:36:79:06:46:86:3B:E8:7A:4A:6A:4C:A5:0E:58:D2:A5:EA:85:E0",
        "17:E5:E2:B4:CA:09:6E:39:C0:33:55:AA:E4:C1:6B:56:11:20:7D:80:84:16:EE:2C:26:96:3D:BF:31:BF:E1:AA"
    };
    public final static String BLOCKEXPLORER_ADDRESS = "https://sandbox.smartbit.com.au/address/";
    public final static String BLOCKEXPLORER_TX = "https://sandbox.smartbit.com.au/tx/";
    public final static String depositPubkey = "036307e560072ed6ce0aa5465534fb5c258a2ccfbc257f369e8e7a181b16d897b3";
    public final static String depositChainCode = "b60befcc619bb1c212732770fe181f2f1aa824ab89f8aab49f2e13e3a56f0f04";
    public final static String GAIT_ONION = "gu5ke7a2aguwfqhz.onion";
    public final static String DEFAULT_PEER = "";
}
