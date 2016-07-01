package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
    public final static String GAIT_WAMP_URL = "wss://prodwss.greenaddress.it/v2/ws/";
    public final static String BLOCKEXPLORER_ADDRESS = "https://www.smartbit.com.au/address/";
    public final static String BLOCKEXPLORER_TX = "https://www.smartbit.com.au/tx/";
    public final static String depositPubkey = "0322c5f5c9c4b9d1c3e22ca995e200d724c2d7d8b6953f7b38fddf9296053c961f";
    public final static String depositChainCode = "e9a563d68686999af372a33157209c6860fe79197a4dafd9ec1dbaa49523351d";
    public final static String GAIT_ONION = "s7a4rvc6425y72d2.onion";
    public final static boolean isAlpha = false;
}
