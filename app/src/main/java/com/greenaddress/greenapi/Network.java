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
    private String name;
    private NetworkParameters networkParameters;
    private boolean liquid;
    private String gaitWampUrl;
    private List<String> gaitWampCertPins;
    private List<BlockExplorer> blockExplorers;
    private String depositPubkey;
    private String depositChainCode;
    private String gaitOnion;
    private List<String> defaultPeers;

    public static Network from(final String json) throws JSONException {
        final InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        return new Network(stream);
    }

    public Network(final InputStream json) throws JSONException {

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            final Map map = objectMapper.readValue(json, Map.class);
            this.name = map.get("name").toString();

            final Object liquid = map.get("liquid");
            this.liquid = liquid==null ? false : (boolean) liquid;
            if (this.liquid) {
                this.networkParameters = ElementsRegTestParams.get();
            } else {
                final Object network = map.get("network");
                switch (network == null ? "mainnet" : network.toString()) {
                    case "mainnet":
                        this.networkParameters = MainNetParams.get();
                        break;
                    case "testnet":
                        this.networkParameters = TestNet3Params.get();
                        break;
                    case "regtest":
                        this.networkParameters = RegTestParams.get();
                        break;
                    default:
                        throw new JSONException("Specified network not recognized (available networks are: mainnet, testnet, regtest)");
                }
            }
            this.gaitWampUrl = map.get("gait_wamp_url").toString();
            this.gaitWampCertPins = (List<String>) map.get("gait_wamp_cert_pins");

            this.blockExplorers = new ArrayList<>();
            for (final Map<String,Object> m : (List<Map<String,Object>>) map.get("blockexplorers")) {
                this.blockExplorers.add(new BlockExplorer(m.get("address").toString(), m.get("tx").toString()));
            }
            this.depositPubkey = map.get("deposit_pubkey").toString();
            this.depositChainCode = map.get("deposit_chain_code").toString();
            this.gaitOnion = map.get("gait_onion").toString();
            this.defaultPeers =  map.get("default_peers") == null ? new ArrayList<>() : (List<String>) map.get("default_peers") ;
        } catch (final IOException e) {
            throw new JSONException("cannot parse network json " + e.getMessage());
        }
    }

    public boolean isElements() {
        return this.networkParameters == ElementsRegTestParams.get();
    }
    public boolean isMainnet() {
        return this.networkParameters == MainNetParams.get();
    }
    public boolean isRegtest() { return this.networkParameters == RegTestParams.get(); }

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
        return blockExplorers.get(0);
    }
    public int getVerPublic() {
        return isMainnet() ? Wally.BIP32_VER_MAIN_PUBLIC : Wally.BIP32_VER_TEST_PUBLIC;
    }
    public int getVerPrivate() {
        return isMainnet() ? Wally.BIP32_VER_MAIN_PRIVATE : Wally.BIP32_VER_TEST_PRIVATE;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Network {");
        sb.append("name='").append(name).append('\'');
        sb.append(", networkParameters=").append(networkParameters);
        sb.append(", liquid=").append(liquid);
        sb.append(", gaitWampUrl='").append(gaitWampUrl).append('\'');
        sb.append(", gaitWampCertPins=").append(gaitWampCertPins);
        sb.append(", blockExplorers=").append(blockExplorers);
        sb.append(", depositPubkey='").append(depositPubkey).append('\'');
        sb.append(", depositChainCode='").append(depositChainCode).append('\'');
        sb.append(", gaitOnion='").append(gaitOnion).append('\'');
        sb.append(", defaultPeers=").append(defaultPeers);
        sb.append('}');
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public NetworkParameters getNetworkParameters() {
        return networkParameters;
    }

    public boolean isLiquid() {
        return liquid;
    }

    public String getGaitWampUrl() {
        return gaitWampUrl;
    }

    public String[] getGaitWampCertPins() {
        return gaitWampCertPins.toArray(new String[gaitWampCertPins.size()]);
    }

    public List<BlockExplorer> getBlockExplorers() {
        return blockExplorers;
    }

    public String getDepositPubkey() {
        return depositPubkey;
    }

    public String getDepositChainCode() {
        return depositChainCode;
    }

    public String getGaitOnion() {
        return gaitOnion;
    }

    public List<String> getDefaultPeers() {
        return defaultPeers;
    }
}
