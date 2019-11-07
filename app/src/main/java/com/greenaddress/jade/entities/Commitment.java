package com.greenaddress.jade.entities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Commitment {
    private byte[] assetId;
    private Long value;
    private byte[] abf;
    private byte[] vbf;
    private byte[] assetGenerator;
    private byte[] valueCommitment;
    private byte[] hmac;

    private byte[] blindingKey;

    @JsonGetter("asset_id")
    public byte[] getAssetId() {
        return assetId;
    }

    @JsonSetter("asset_id")
    public void setAssetId(byte[] assetId) {
        this.assetId = assetId;
    }

    @JsonGetter("value")
    public Long getValue() {
        return value;
    }

    @JsonSetter("value")
    public void setValue(Long value) {
        this.value = value;
    }

    @JsonGetter("abf")
    public byte[] getAbf() {
        return abf;
    }

    @JsonSetter("abf")
    public void setAbf(byte[] abf) {
        this.abf = abf;
    }

    @JsonGetter("vbf")
    public byte[] getVbf() {
        return vbf;
    }

    @JsonSetter("vbf")
    public void setVbf(byte[] vbf) {
        this.vbf = vbf;
    }

    @JsonGetter("asset_generator")
    public byte[] getAssetGenerator() {
        return assetGenerator;
    }

    @JsonSetter("asset_generator")
    public void setAssetGenerator(byte[] assetGenerator) {
        this.assetGenerator = assetGenerator;
    }

    @JsonGetter("value_commitment")
    public byte[] getValueCommitment() {
        return valueCommitment;
    }

    @JsonSetter("value_commitment")
    public void setValueCommitment(byte[] valueCommitment) {
        this.valueCommitment = valueCommitment;
    }

    @JsonGetter("hmac")
    public byte[] getHmac() {
        return hmac;
    }

    @JsonSetter("hmac")
    public void setHmac(byte[] hmac) {
        this.hmac = hmac;
    }

    @JsonGetter("blinding_key")
    public byte[] getBlindingKey() {
        return blindingKey;
    }

    @JsonSetter("blinding_key")
    public void setBlindingKey(byte[] blindingKey) {
        this.blindingKey = blindingKey;
    }
}
