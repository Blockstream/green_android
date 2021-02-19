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
    private String assetId;
    private String assetblinder;
    private Long satoshi;
    private String amountblinder;

    InputUnblindedData(final Long vin, final String assetId, final String assetblinder, final Long satoshi, final String amountblinder) {
        super();
        setVin(vin);
        setAssetId(assetId);
        setAssetblinder(assetblinder);
        setSatoshi(satoshi);
        setAmountblinder(amountblinder);
    }

    public Long getVin() {
        return vin;
    }

    public void setVin(final Long vin) {
        this.vin = vin;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(final String assetId) {
        this.assetId = assetId;
    }

    public Long getSatoshi() {
        return satoshi;
    }

    public void setSatoshi(final Long satoshi) {
        this.satoshi = satoshi;
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
