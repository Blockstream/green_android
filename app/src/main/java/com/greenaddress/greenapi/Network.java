package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;
import com.greenaddress.greenbits.ui.BuildConfig;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Network {
    private String mName;
    private NetworkParameters mNetworkParameters;
    private boolean mLiquid;
    private String mWampUrl;
    private List<String> mWampCertPins;
    private List<BlockExplorer> mBlockExplorers;
    private String mDepositPubkey;
    private String mDepositChainCode;
    private String mOnion;
    private List<String> mDefaultPeers;

    public static Network from(final String json) throws GAException {
        final InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        return new Network(stream);
    }

    public Network(final InputStream json) throws GAException {

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            final Map<String,Object> map = objectMapper.readValue(json, Map.class);
            final JSONMap jsonMap = new JSONMap(map);
            mName = jsonMap.getString("name");
            mLiquid = jsonMap.getBool("liquid");

            if (!mLiquid) {
                final String network = jsonMap.getString("network");
                switch (network == null ? "mainnet" : network) {
                    case "mainnet":
                        mNetworkParameters = MainNetParams.get();
                        break;
                    case "testnet":
                        mNetworkParameters = TestNet3Params.get();
                        break;
                    case "regtest":
                        mNetworkParameters = RegTestParams.get();
                        break;
                    default:
                        throw new GAException("Invalid Network");
                }
            }
            else
                mNetworkParameters = ElementsRegTestParams.get();

            mWampUrl = jsonMap.getString("wamp_url");
            mWampCertPins = (List<String>) jsonMap.get("wamp_cert_pins");
            mOnion = jsonMap.getString("wamp_url_onion");
            mBlockExplorers = new ArrayList<>();
            for (final Map<String,Object> m : (List<Map<String,Object>>) jsonMap.get("blockexplorers"))
                mBlockExplorers.add(new BlockExplorer(m.get("address").toString(), m.get("tx").toString()));

            mDepositPubkey = jsonMap.getString("deposit_pubkey");
            mDepositChainCode = jsonMap.getString("deposit_chain_code");

            mDefaultPeers =  jsonMap.get("default_peers") == null ? new ArrayList<>() : (List<String>) jsonMap.mData.get("default_peers") ;
        } catch (final IOException e) {
            throw new GAException("Invalid Network");
        }
    }

    public boolean isElements() {
        return mNetworkParameters == ElementsRegTestParams.get();
    }
    public boolean isMainnet() {
        return mNetworkParameters == MainNetParams.get();
    }
    public boolean isRegtest() { return mNetworkParameters == RegTestParams.get(); }

    public int getBip32Network() {
        return isMainnet() ? Wally.BIP38_KEY_MAINNET : Wally.BIP38_KEY_TESTNET;
    }
    public int getBip38Flags() {
        return getBip32Network() | Wally.BIP38_KEY_COMPRESSED;
    }
    public boolean alwaysAllowRBF() {
        return BuildConfig.DEBUG && isRegtest();
    }
    public BlockExplorer getFirstBlockExplorer() {
        return mBlockExplorers.get(0);
    }
    public int getVerPublic() {
        return isMainnet() ? Wally.BIP32_VER_MAIN_PUBLIC : Wally.BIP32_VER_TEST_PUBLIC;
    }
    public int getVerPrivate() {
        return isMainnet() ? Wally.BIP32_VER_MAIN_PRIVATE : Wally.BIP32_VER_TEST_PRIVATE;
    }

    public String getName() {
        return mName;
    }

    public NetworkParameters getNetworkParameters() {
        return mNetworkParameters;
    }

    public boolean isLiquid() {
        return mLiquid;
    }

    public String getWampUrl() {
        return mWampUrl;
    }

    public String[] getWampCertPins() {
        return mWampCertPins.toArray(new String[mWampCertPins.size()]);
    }

    public List<BlockExplorer> getBlockExplorers() {
        return mBlockExplorers;
    }

    public String getDepositPubkey() {
        return mDepositPubkey;
    }

    public String getDepositChainCode() {
        return mDepositChainCode;
    }

    public String getOnion() {
        return mOnion;
    }

    public List<String> getDefaultPeers() {
        return mDefaultPeers;
    }
}
