package com.greenaddress.greenapi.data;

import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputOutputData extends JSONData {
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

    @JsonIgnore
    public byte[] getTxid() {
        final byte[] buf = Wally.hex_to_bytes(txhash);
        for (int i = 0; i < buf.length / 2; ++i) {
            byte b = buf[i];
            buf[i] = buf[buf.length - i - 1];
            buf[buf.length - i - 1] = b;
        }
        return buf;
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
}
