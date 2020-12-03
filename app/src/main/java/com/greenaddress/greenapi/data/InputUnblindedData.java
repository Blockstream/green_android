package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.io.Serializable;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputUnblindedData extends JSONData implements Serializable {
    private Long vin;
    private String asset;
    private String assetblinder;
    private Long amount;
    private String amountblinder;

    InputUnblindedData(final Long vin, final String asset, final String assetblinder, final Long amount, final String amountblinder) {
        super();
        setVin(vin);
        setAsset(asset);
        setAssetblinder(assetblinder);
        setAmount(amount);
        setAmountblinder(amountblinder);
    }

    public Long getVin() {
        return vin;
    }

    public void setVin(final Long vin) {
        this.vin = vin;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(final String asset) {
        this.asset = asset;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(final Long amount) {
        this.amount = amount;
    }

    public String getAmountblinder() {
        return amountblinder;
    }

    public void setAmountblinder(final String amountblinder) {
        this.amountblinder = amountblinder;
    }

    public String getAssetblinder() {
        return assetblinder;
    }

    public void setAssetblinder(String assetblinder) {
        this.assetblinder = assetblinder;
    }
}