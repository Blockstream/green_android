package com.greenaddress.greenapi.data;

import androidx.annotation.NonNull;

import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.R;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;

import java.util.List;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class NetworkData extends JSONData implements Comparable<NetworkData> {
    private String addressExplorerUrl;
    private String txExplorerUrl;
    private String bech32Prefix;
    private Boolean development;
    private Boolean liquid;
    private Boolean mainnet;
    private String name;
    private String network;
    private Integer p2pkhVersion;
    private Integer p2shVersion;
    private String serviceChainCode;
    private String servicePubkey;
    private String wampOnionUrl;
    private String wampUrl;
    private List<String> wampCertPins;
    private List<String> wampCertRoots;
    private List<String> defaultPeers;
    private Integer blindedPrefix;
    private Integer ctBits;
    private Integer ctExponent;
    private String policyAsset;
    private String serverType;

    @JsonIgnore
    public NetworkParameters getNetworkParameters() {
        switch (network == null ? "mainnet" : network) {
        case "mainnet":
            return MainNetParams.get();
        case "testnet":
            return TestNet3Params.get();
        case "regtest":
        case "localtest":
            return RegTestParams.get();
        default:
            return null;
        }
    }

    @JsonIgnore
    public static NetworkData find(final String networkName, final List<NetworkData> list) {
        for (NetworkData n : list)
            if (n.getNetwork().equals(networkName))
                return n;
        return null;
    }

    @JsonIgnore
    public boolean IsNetworkMainnet() {
        return getMainnet();
    }

    @JsonIgnore
    public boolean isTestnet() {
        return getNetworkParameters() == TestNet3Params.get();
    }

    @JsonIgnore
    public boolean isRegtest() { return getNetworkParameters() == RegTestParams.get(); }

    @JsonIgnore
    public int getBip32Network() {
        return IsNetworkMainnet() ? Wally.BIP38_KEY_MAINNET : Wally.BIP38_KEY_TESTNET;
    }

    @JsonIgnore
    public int getBip38Flags() {
        return getBip32Network() | Wally.BIP38_KEY_COMPRESSED;
    }

    @JsonIgnore
    public boolean alwaysAllowRBF() {
        return BuildConfig.DEBUG || isRegtest() || isTestnet();
    }

    @JsonIgnore
    public int getVerPublic() {
        return IsNetworkMainnet() ? Wally.BIP32_VER_MAIN_PUBLIC : Wally.BIP32_VER_TEST_PUBLIC;
    }

    @JsonIgnore
    public int getVerPrivate() {
        return IsNetworkMainnet() ? Wally.BIP32_VER_MAIN_PRIVATE : Wally.BIP32_VER_TEST_PRIVATE;
    }

    @JsonIgnore
    public int getIcon() {
        if (network.equals("mainnet"))
            return R.drawable.ic_btc;
        if (network.equals("testnet"))
            return R.drawable.ic_testnet_btc;
        if (network.equals("localtest-liquid"))
            return R.drawable.ic_liquid;
        if (network.equals("liquid"))
            return R.drawable.ic_liquid;
        return R.drawable.ic_testnet_btc;
    }

    @JsonIgnore
    public boolean isElectrum() { return "electrum".equals(getServerType()); }

    public String getPolicyAsset() {
        return policyAsset;
    }

    public void setPolicyAsset(String policyAsset) {
        this.policyAsset = policyAsset;
    }

    public Integer getCtBits() {
        return ctBits;
    }

    public void setCtBits(Integer ctBits) {
        this.ctBits = ctBits;
    }

    public Integer getCtExponent() {
        return ctExponent;
    }

    public void setCtExponent(Integer ctExponent) {
        this.ctExponent = ctExponent;
    }

    public Integer getBlindedPrefix() {
        return blindedPrefix;
    }

    public void setBlindedPrefix(Integer blindedPrefix) {
        this.blindedPrefix = blindedPrefix;
    }

    public Boolean getDevelopment() {
        return development;
    }

    public void setDevelopment(Boolean development) {
        this.development = development;
    }

    public String getAddressExplorerUrl() {
        return addressExplorerUrl;
    }

    public void setAddressExplorerUrl(final String addressExplorerUrl) {
        this.addressExplorerUrl = addressExplorerUrl;
    }

    public String getBech32Prefix() {
        return bech32Prefix;
    }

    public void setBech32Prefix(final String bech32Prefix) {
        this.bech32Prefix = bech32Prefix;
    }

    public Boolean getLiquid() {
        return liquid;
    }

    public void setLiquid(final Boolean liquid) {
        this.liquid = liquid;
    }

    public Boolean getMainnet() {
        return mainnet;
    }

    public void setMainnet(final Boolean mainnet) {
        this.mainnet = mainnet;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(final String network) {
        this.network = network;
    }

    public Integer getP2pkhVersion() {
        return p2pkhVersion;
    }

    public void setP2pkhVersion(final Integer p2pkhVersion) {
        this.p2pkhVersion = p2pkhVersion;
    }

    public Integer getP2shVersion() {
        return p2shVersion;
    }

    public void setP2shVersion(final Integer p2shVersion) {
        this.p2shVersion = p2shVersion;
    }

    public String getServiceChainCode() {
        return serviceChainCode;
    }

    public void setServiceChainCode(final String serviceChainCode) {
        this.serviceChainCode = serviceChainCode;
    }

    public String getServicePubkey() {
        return servicePubkey;
    }

    public void setServicePubkey(final String servicePubkey) {
        this.servicePubkey = servicePubkey;
    }

    public String getTxExplorerUrl() {
        return txExplorerUrl;
    }

    public void setTxExplorerUrl(final String txExplorerUrl) {
        this.txExplorerUrl = txExplorerUrl;
    }

    public String getWampOnionUrl() {
        return wampOnionUrl;
    }

    public void setWampOnionUrl(final String wampOnionUrl) {
        this.wampOnionUrl = wampOnionUrl;
    }

    public String getWampUrl() {
        return wampUrl;
    }

    public void setWampUrl(final String wampUrl) {
        this.wampUrl = wampUrl;
    }

    public List<String> getWampCertPins() {
        return wampCertPins;
    }

    public void setWampCertPins(final List<String> wampCertPins) {
        this.wampCertPins = wampCertPins;
    }

    public List<String> getWampCertRoots() {
        return wampCertRoots;
    }

    public void setWampCertRoots(final List<String> wampCertRoots) {
        this.wampCertRoots = wampCertRoots;
    }

    public List<String> getDefaultPeers() {
        return defaultPeers;
    }

    public void setDefaultPeers(final List<String> defaultPeers) {
        this.defaultPeers = defaultPeers;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(final String serverType) {
        this.serverType = serverType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkData that = (NetworkData) o;
        return Objects.equals(network, that.network);
    }

    @Override
    public int hashCode() {

        return Objects.hash(network);
    }

    @Override
    public int compareTo(@NonNull NetworkData o) {
        return getName().compareTo(o.getName());
    }
}
