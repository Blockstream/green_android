package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.Serializable;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceData extends JSONData implements Serializable {
    //{"bits":"1584407.51","btc":"1.58440751","fiat":"1.74284826100000014072365527239843",
    // "fiat_currency":"USD","fiat_rate":"1.10000000000000008881784197001252","mbtc":"1584.40751",
    // "satoshi":158440751,"ubtc":"1584407.51"}
    private String address;
    private String bits;
    private String btc;
    private String fiat;
    private String fiatCurrency;
    private String fiatRate;
    private String mbtc;
    private String ubtc;
    private String sats;
    private Long satoshi;
    private String asset;
    private String assetValue;
    private AssetInfoData assetInfo;

    @JsonIgnore
    public static BalanceData from(final ObjectMapper objectMapper,
                                   final ObjectNode objectNode) throws JsonProcessingException {
        final BalanceData balance = objectMapper.treeToValue(objectNode, BalanceData.class);
        if (balance.assetInfo != null && balance.assetInfo.getAssetId() != null) {
            balance.asset = balance.assetInfo.getAssetId();
            balance.assetValue = objectNode.get(balance.asset).asText();
        }
        return balance;
    }

    @JsonIgnore
    public ObjectNode toObjectNode() {
        return new ObjectMapper().convertValue(this, ObjectNode.class);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBits() {
        return bits;
    }

    public void setBits(String bits) {
        this.bits = bits;
    }

    public String getBtc() {
        return btc;
    }

    public void setBtc(String btc) {
        this.btc = btc;
    }

    public String getFiat() {
        return fiat;
    }

    public void setFiat(String fiat) {
        this.fiat = fiat;
    }

    public String getFiatCurrency() {
        return fiatCurrency;
    }

    public void setFiatCurrency(final String fiatCurrency) {
        this.fiatCurrency = fiatCurrency;
    }

    public String getFiatRate() {
        return fiatRate;
    }

    public void setFiatRate(final String fiatRate) {
        this.fiatRate = fiatRate;
    }

    public String getMbtc() {
        return mbtc;
    }

    public void setMbtc(final String mbtc) {
        this.mbtc = mbtc;
    }

    public String getUbtc() {
        return ubtc;
    }

    public void setUbtc(final String ubtc) {
        this.ubtc = ubtc;
    }

    public String getSats() {return sats; }

    public void setSats(final String sats) { this.sats = sats; }

    public Long getSatoshi() {
        return satoshi;
    }

    public void setSatoshi(final Long satoshi) {
        this.satoshi = satoshi;
    }

    public AssetInfoData getAssetInfo() {
        return assetInfo;
    }

    public void setAssetInfo(final AssetInfoData assetInfo) {
        this.assetInfo = assetInfo;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(final String asset) {
        this.asset = asset;
    }

    public String getAssetValue() {
        return assetValue;
    }

    public void setAssetValue(final String assetValue) {
        this.assetValue = assetValue;
    }
}
