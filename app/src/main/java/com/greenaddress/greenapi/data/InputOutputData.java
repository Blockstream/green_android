package com.greenaddress.greenapi.data;

import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputOutputData extends JSONData implements Serializable {
    private String address;
    private String addressee;
    private String addressType;
    private Boolean isChange;
    private Boolean isOutput;
    private Boolean isRelevant;
    private Boolean isSpent;
    private Integer pointer;
    private String prevoutScript;
    private Long ptIdx;
    private String recoveryXpub;
    private Long satoshi;
    private String script;
    private Integer scriptType;
    private Long sequence;
    private Integer subaccount;
    private Integer subtype;
    private String txhash;
    private String serviceXpub;
    private List<Long> userPath;
    private String commitment; // blinded value
    private String assetblinder; // asset blinding factor
    private String amountblinder; // value blinding factor
    private String assetId; // asset id for Liquid txs
    private String publicKey; // the pubkey embedded into the blinded address we are sending to
    private String ephKeypairSec; // our secret key used for the blinding
    private String ephKeypairPub; // and the public key

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public String getAddressee() {
        return addressee;
    }

    public void setAddressee(final String addressee) {
        this.addressee = addressee;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public Boolean getIsChange() { return isChange; }

    public void setIsChange(final Boolean change) { isChange = change; }

    public Boolean getIsOutput() {
        return isOutput;
    }

    public void setIsOutput(Boolean output) {
        isOutput = output;
    }

    public Boolean getIsRelevant() {
        return isRelevant;
    }

    public void setIsRelevant(Boolean relevant) {
        isRelevant = relevant;
    }

    public Boolean getIsSpent() {
        return isSpent;
    }

    public void setIsSpent(Boolean spent) {
        isSpent = spent;
    }

    public Integer getPointer() {
        return pointer;
    }

    public void setPointer(final Integer pointer) {
        this.pointer = pointer;
    }

    public String getPrevoutScript() { return prevoutScript; }

    public void setPrevoutScript(String prevoutScript) { this.prevoutScript = prevoutScript; }

    public Long getPtIdx() {
        return ptIdx;
    }

    public void setPtIdx(final Long ptIdx) {
        this.ptIdx = ptIdx;
    }

    public String getRecoveryXpub() { return recoveryXpub; }

    public void setRecoveryXpub(final String recoveryXpub) {
        this.recoveryXpub = recoveryXpub;
    }

    public Long getSatoshi() {
        return satoshi;
    }

    public void setSatoshi(final Long satoshi) {
        this.satoshi = satoshi;
    }

    public String getScript() { return script; }

    public void setScript(String script) { this.script = script; }

    public Integer getScriptType() {
        return scriptType;
    }

    public void setScriptType(final Integer scriptType) {
        this.scriptType = scriptType;
    }

    public Long getSequence() { return sequence; }

    public void setSequence(final Long sequence) { this.sequence = sequence; }

    public Integer getSubaccount() {
        return subaccount;
    }

    public void setSubaccount(final Integer subaccount) {
        this.subaccount = subaccount;
    }

    public Integer getSubtype() { return subtype; }

    public void setSubtype(Integer subtype) { this.subtype = subtype; }

    public String getCommitment() {
        return commitment;
    }

    public void setCommitment(String commitment) {
        this.commitment = commitment;
    }

    public String getAssetblinder() {
        return assetblinder;
    }

    public void setAssetblinder(String assetblinder) {
        this.assetblinder = assetblinder;
    }

    public String getAmountblinder() {
        return amountblinder;
    }

    public void setAmountblinder(String amountblinder) {
        this.amountblinder = amountblinder;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @JsonIgnore
    public byte[] getCommitmentBytes() {
        return Wally.hex_to_bytes(commitment);
    }

    @JsonIgnore
    public byte[] getAssetBlinderBytes() {
        return reverseBytes(Wally.hex_to_bytes(assetblinder));
    }

    @JsonIgnore
    public byte[] getAmountBlinderBytes() {
        return reverseBytes(Wally.hex_to_bytes(amountblinder));
    }

    @JsonIgnore
    public byte[] getAssetIdBytes() {
        return Wally.hex_to_bytes(assetId);
    }

    @JsonIgnore
    public byte[] getRevertedAssetIdBytes() {
        return reverseBytes(Wally.hex_to_bytes(assetId));
    }

    public String getEphKeypairSec() {
        return ephKeypairSec;
    }

    @JsonIgnore
    public byte[] getEphKeypairSecBytes() {
        return Wally.hex_to_bytes(ephKeypairSec);
    }

    public String getEphKeypairPub() {
        return ephKeypairPub;
    }

    @JsonIgnore
    public byte[] getEphKeypairPubBytes() {
        return Wally.hex_to_bytes(ephKeypairPub);
    }

    @JsonIgnore
    public byte[] getPublicKeyBytes() {
        return Wally.hex_to_bytes(publicKey);
    }

    // FIXME: Put this somewhere common
    @JsonIgnore
    public static byte[] reverseBytes(final byte[] buf) {
        for (int i = 0; i < buf.length / 2; ++i) {
            byte b = buf[i];
            buf[i] = buf[buf.length - i - 1];
            buf[buf.length - i - 1] = b;
        }
        return buf;
    }

    @JsonIgnore
    public byte[] getTxid() {
        return reverseBytes(Wally.hex_to_bytes(txhash));
    }

    public String getTxhash() { return txhash; }

    public void setTxhash(String txhash) { this.txhash = txhash; }

    public String getServiceXpub() {
        return serviceXpub;
    }

    public void setServiceXpub(final String serviceXpub) {
        this.serviceXpub = serviceXpub;
    }

    public List<Long> getUserPath() { return userPath; }

    public void setUserPath(final List<Long> userPath) { this.userPath = userPath; }

    @JsonIgnore
    public List<Integer> getUserPathAsInts() {
        final List<Integer> path = new ArrayList<>(userPath.size());
        for (final Long v : userPath) {
            path.add((int) (long) v);
        }
        return path;
    }

    // Return blinded data as expected by the explorer
    public String getUnblindedString() {
        if (hasUnblindingData()) {
            // <value_in_satoshi>,<asset_tag_hex>,<amount_blinder_hex>,<asset_blinder_hex>
            return String.format("%ld,%s,%s,%s",satoshi, assetId, amountblinder, assetblinder);
        }
        return "";
    }

    public boolean hasUnblindingData() {
        return assetId != null && satoshi != null && assetblinder != null && amountblinder != null &&  !assetId.isEmpty() && !amountblinder.isEmpty() && !assetblinder.isEmpty();
    }
}
