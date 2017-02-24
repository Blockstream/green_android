package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
    public final static String GAIT_WAMP_URL = "wss://prodwss.greenaddress.it/v2/ws/";
    public final static String[] GAIT_WAMP_CERT_PINS = {
        // Let’s Encrypt Authority X3:
        "25:84:7D:66:8E:B4:F0:4F:DD:40:B1:2B:6B:07:40:C5:67:DA:7D:02:43:08:EB:6C:2C:96:FE:41:D9:DE:21:8D",
        // Let’s Encrypt Authority X4: (backup)
        "A7:4B:0C:32:B6:5B:95:FE:2C:4F:8F:09:89:47:A6:8B:69:50:33:BE:D0:B5:1D:D8:B9:84:EC:AE:89:57:1B:B6"
    };
    public final static String BLOCKEXPLORER_ADDRESS = "https://www.smartbit.com.au/address/";
    public final static String BLOCKEXPLORER_TX = "https://www.smartbit.com.au/tx/";
    public final static String depositPubkey = "0322c5f5c9c4b9d1c3e22ca995e200d724c2d7d8b6953f7b38fddf9296053c961f";
    public final static String depositChainCode = "e9a563d68686999af372a33157209c6860fe79197a4dafd9ec1dbaa49523351d";
    public final static String GAIT_ONION = "s7a4rvc6425y72d2.onion";
    public final static String DEFAULT_PEER = "";
}
