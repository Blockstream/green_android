package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    public final static String GAIT_TOKEN_URL = "https://segwit.greenaddress.it/token/";
    public final static String GAIT_WAMP_URL = "wss://segwitwss.greenaddress.it/ws/inv";
    public final static String BLOCKEXPLORER = "https://notdevelopedyetsorry.com";
    public final static String depositPubkey = "036307e560072ed6ce0aa5465534fb5c258a2ccfbc257f369e8e7a181b16d897b3";
    public final static String depositChainCode = "b60befcc619bb1c212732770fe181f2f1aa824ab89f8aab49f2e13e3a56f0f04";
}
