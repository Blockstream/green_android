package com.greenaddress.greenapi.data;

import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubaccountData extends JSONData {
    private String name;
    private Integer pointer;
    private Boolean hasTransactions;
    private String receivingId;
    private String recoveryChainCode;
    private String recoveryPubKey;
    private String type;  // 2of3 2of2

    @JsonIgnore
    public byte[] getRecoveryPubKeyAsBytes() {
        return Wally.hex_to_bytes(getRecoveryPubKey());
    }

    @JsonIgnore
    public byte[] getRecoveryChainCodeAsBytes() {
        return Wally.hex_to_bytes(getRecoveryChainCode());
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public String getNameWithDefault(String s) {
        return name.isEmpty() ? s : name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getPointer() {
        return pointer;
    }

    public void setPointer(final Integer pointer) {
        this.pointer = pointer;
    }

    public Boolean getHasTransactions() {
        return hasTransactions;
    }

    public void setHasTransactions(final Boolean hasTransactions) {
        this.hasTransactions = hasTransactions;
    }

    public String getReceivingId() {
        return receivingId;
    }

    public void setReceivingId(final String receivingId) {
        this.receivingId = receivingId;
    }

    public String getRecoveryChainCode() {
        return recoveryChainCode;
    }

    public void setRecoveryChainCode(final String recoveryChainCode) {
        this.recoveryChainCode = recoveryChainCode;
    }

    public String getRecoveryPubKey() {
        return recoveryPubKey;
    }

    public void setRecoveryPubKey(final String recoveryPubKey) {
        this.recoveryPubKey = recoveryPubKey;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
