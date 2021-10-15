package com.greenaddress.greenapi.data;

import androidx.annotation.NonNull;

import com.blockstream.crypto.BuildConfig;
import com.blockstream.crypto.R;
import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class NetworkData extends JSONData implements Comparable<NetworkData>, Serializable {
    private String txExplorerUrl;
    private Boolean development;
    private Boolean liquid;
    private Boolean mainnet;
    private String name;
    private String network;
    private List<String> defaultPeers;
    private String policyAsset;
    private String serverType;
    private List<Integer> csvBuckets;

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
        return network.contains("testnet");
    }

    @JsonIgnore
    public boolean isRegtest() {
        return "regtest".equals(network) || "localtest".equals(network);
    }

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
        if (network.equals("mainnet") || network.equals("electrum-mainnet"))
            return R.drawable.ic_btc;
        if (network.equals("testnet") || network.equals("electrum-testnet"))
            return R.drawable.ic_testnet_btc;
        if (network.equals("localtest-liquid"))
            return R.drawable.ic_liquid;
        if (network.equals("liquid") || network.equals("electrum-liquid"))
            return R.drawable.ic_liquid;
        return R.drawable.ic_testnet_btc;
    }

    @JsonIgnore
    public boolean isElectrum() { return "electrum".equals(getServerType()); }

    // Return the identifier of the asset used to pay transaction fees
    public String getPolicyAsset() {
        return liquid ? policyAsset : "btc";
    }

    public void setPolicyAsset(String policyAsset) {
        this.policyAsset = policyAsset;
    }

    public Boolean getDevelopment() {
        return development;
    }

    public void setDevelopment(Boolean development) {
        this.development = development;
    }

    public Boolean getLiquid() {
        return liquid;
    }

    public Boolean canReplaceTransactions(){
        return !getLiquid();
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

    public String getTxExplorerUrl() {
        return txExplorerUrl;
    }

    public void setTxExplorerUrl(final String txExplorerUrl) {
        this.txExplorerUrl = txExplorerUrl;
    }

    public List<String> getDefaultPeers() {
        return defaultPeers;
    }

    public void setDefaultPeers(final List<String> defaultPeers) {
        this.defaultPeers = defaultPeers;
    }

    public List<Integer> getCsvBuckets() {
        return csvBuckets;
    }

    public void setCsvBuckets(final List<Integer> csvBuckets) {
        this.csvBuckets = csvBuckets;
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
