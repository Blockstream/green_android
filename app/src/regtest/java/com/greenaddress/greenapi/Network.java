package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
    public final static String GAIT_TOKEN_URL = "http://10.0.2.2:9908/token/";
    public final static String GAIT_WAMP_URL = "ws://10.0.2.2:8080/v2/ws/";
    public final static String BLOCKEXPLORER = "http://192.168.56.1:8080";
    public final static String depositChainCode = "b60befcc619bb1c212732770fe181f2f1aa824ab89f8aab49f2e13e3a56f0f04";
    public final static String depositPubkey = "036307e560072ed6ce0aa5465534fb5c258a2ccfbc257f369e8e7a181b16d897b3";
    public final static String GAIT_ONION = "";

}
