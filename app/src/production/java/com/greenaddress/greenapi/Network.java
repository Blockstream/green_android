package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
    public final static String GAIT_WAMP_URL = "wss://prodwss.greenaddress.it/v2/ws/";
    public final static String[] GAIT_WAMP_CERT_PINS = {
        "38:6B:44:0D:E8:26:C4:36:E4:39:A0:D4:3F:D2:15:CE:E2:5B:C5:64:2A:54:87:E7:34:A0:CD:0B:B5:5A:FB:A2",
        "91:0E:75:35:FB:21:88:C2:88:9B:89:06:BF:92:06:31:25:DC:1E:28:A7:EA:85:D3:E6:05:FC:63:35:B4:68:27"
    };
    public final static String BLOCKEXPLORER_ADDRESS = "https://www.smartbit.com.au/address/";
    public final static String BLOCKEXPLORER_TX = "https://www.smartbit.com.au/tx/";
    public final static String depositPubkey = "0322c5f5c9c4b9d1c3e22ca995e200d724c2d7d8b6953f7b38fddf9296053c961f";
    public final static String depositChainCode = "e9a563d68686999af372a33157209c6860fe79197a4dafd9ec1dbaa49523351d";
    public final static String GAIT_ONION = "s7a4rvc6425y72d2.onion";
    public final static String DEFAULT_PEER = "";
}
