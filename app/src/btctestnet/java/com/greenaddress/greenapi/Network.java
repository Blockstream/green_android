package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    public final static String GAIT_WAMP_URL = "wss://testwss.greenaddress.it/v2/ws/";
    public final static String[] GAIT_WAMP_CERT_PINS = {"BE:26:87:C9:37:98:B3:4E:70:22:0D:18:FE:9E:39:7F:15:7E:0A:F8"};
    public final static String BLOCKEXPLORER_ADDRESS = "https://sandbox.smartbit.com.au/address/";
    public final static String BLOCKEXPLORER_TX = "https://sandbox.smartbit.com.au/tx/";
    public final static String depositPubkey = "036307e560072ed6ce0aa5465534fb5c258a2ccfbc257f369e8e7a181b16d897b3";
    public final static String depositChainCode = "b60befcc619bb1c212732770fe181f2f1aa824ab89f8aab49f2e13e3a56f0f04";
    public final static String GAIT_ONION = "gu5ke7a2aguwfqhz.onion";
    public final static String DEFAULT_PEER = "";
}
