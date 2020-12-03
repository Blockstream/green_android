package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HardwareCodeResolverData extends JSONData {
    private List<String> xpubs;
    private String signature;
    private List<String> signatures;
    private String blindingKey;
    private List<String> nonces;
    private Map<String, String> blindingKeys;
    private List<String> assetCommitments;
    private List<String> valueCommitments;
    private List<String> assetblinders;
    private List<String> amountblinders;

    public List<String> getXpubs() {
        return xpubs;
    }

    public void setXpubs(List<String> xpubs) {
        this.xpubs = xpubs;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public List<String> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<String> signatures) {
        this.signatures = signatures;
    }

    public String getBlindingKey() {
        return blindingKey;
    }

    public void setBlindingKey(String blindingKey) {
        this.blindingKey = blindingKey;
    }

    public List<String> getNonces() {
        return nonces;
    }

    public void setNonces(List<String> nonces) {
        this.nonces = nonces;
    }

    public Map<String, String> getBlindingKeys() {
        return blindingKeys;
    }

    public void setBlindingKeys(Map<String, String> blindingKeys) {
        this.blindingKeys = blindingKeys;
    }

    public void setAssetCommitments(List<String> assetCommitments) {
        this.assetCommitments = assetCommitments;
    }

    public void setValueCommitments(List<String> valueCommitments) {
        this.valueCommitments = valueCommitments;
    }

    public List<String> getAssetCommitments() {
        return assetCommitments;
    }

    public List<String> getValueCommitments() {
        return valueCommitments;
    }

    public void setAssetblinders(List<String> assetblinders) {
        this.assetblinders = assetblinders;
    }

    public void setAmountblinders(List<String> amountblinders) {
        this.amountblinders = amountblinders;
    }

    public List<String> getAssetblinders() {
        return assetblinders;
    }

    public List<String> getAmountblinders() {
        return amountblinders;
    }
}
