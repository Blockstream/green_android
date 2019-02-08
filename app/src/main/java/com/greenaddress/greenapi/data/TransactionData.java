package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;

import java.util.Date;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionData extends JSONData {
    private Integer blockHeight;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
    private Long fee;
    private Long feeRate;
    private String memo;
    private Integer subaccount;
    private List<Integer> subaccounts;
    private String txhash;
    private Long satoshi;
    private String type;
    private Boolean canRbf;
    private Boolean canCpfp;
    private Boolean rbfOptin;
    private String privateKey;
    private Boolean instant;
    private String transaction;
    private Integer transactionSize;
    private Integer transactionVsize;
    private Integer transactionWeight;
    private Boolean serverSigned;
    private Boolean userSigned;
    private String data;
    // FIXME: I don't think these belong here
    private Integer ptIdx;
    private Integer scriptType;
    private String addressType;
    private Integer pointer;
    private Long gaAssetId;
    private Long subtype;
    // END FIXME
    private Boolean hasPaymentRequest;
    private List<String> addressees;
    private List<InputOutputData> inputs;
    private List<InputOutputData> outputs;


    @JsonIgnore
    public Coin getSatoshiAsCoin() {
        return satoshi == null ? null : Coin.valueOf(satoshi);
    }

    @JsonIgnore
    public Sha256Hash getTxhashAsSha256Hash() {
        return txhash == null ? null : Sha256Hash.wrap(txhash);
    }

    @JsonIgnore
    public static TransactionData find(final List<TransactionData> transactions, final String txhash) {
        for (TransactionData t : transactions) {
            if (t.getTxhash().equals(txhash))
                return t;
        }
        return null;
    }

    @JsonIgnore
    public String getAddressee() {
        return getAddressees().isEmpty() ? "" : getAddressees().get(0);
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public Boolean getRbfOptin() {
        return rbfOptin;
    }

    public void setRbfOptin(Boolean rbfOptin) {
        this.rbfOptin = rbfOptin;
    }

    public Boolean getInstant() {
        return instant;
    }

    public void setInstant(Boolean instant) {
        this.instant = instant;
    }

    public Boolean getHasPaymentRequest() {
        return hasPaymentRequest;
    }

    public void setHasPaymentRequest(Boolean hasPaymentRequest) {
        this.hasPaymentRequest = hasPaymentRequest;
    }

    public Long getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(Long feeRate) {
        this.feeRate = feeRate;
    }

    public Boolean getCanRbf() {
        return canRbf;
    }

    public void setCanRbf(Boolean canRbf) {
        this.canRbf = canRbf;
    }

    public Boolean getCanCpfp() {
        return canCpfp;
    }

    public void setCanCpfp(Boolean canCpfp) {
        this.canCpfp = canCpfp;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public Integer getTransactionSize() {
        return transactionSize;
    }

    public void setTransactionSize(Integer transactionSize) {
        this.transactionSize = transactionSize;
    }

    public Integer getTransactionVsize() {
        return transactionVsize;
    }

    public void setTransactionVsize(Integer transactionVsize) {
        this.transactionVsize = transactionVsize;
    }

    public Integer getTransactionWeight() {
        return transactionWeight;
    }

    public void setTransactionWeight(Integer transactionWeight) {
        this.transactionWeight = transactionWeight;
    }

    public Boolean getServerSigned() {
        return serverSigned;
    }

    public void setServerSigned(Boolean serverSigned) {
        this.serverSigned = serverSigned;
    }

    public Boolean getUserSigned() {
        return userSigned;
    }

    public void setUserSigned(Boolean userSigned) {
        this.userSigned = userSigned;
    }

    public List<String> getAddressees() {
        return addressees;
    }

    public void setAddressees(final List<String> addressees) {
        this.addressees = addressees;
    }

    public List<InputOutputData> getInputs() {
        return inputs;
    }

    public void setInputs(final List<InputOutputData> inputs) {
        this.inputs = inputs;
    }

    public List<InputOutputData> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<InputOutputData> outputs) {
        this.outputs = outputs;
    }

    public Integer getPtIdx() {
        return ptIdx;
    }

    public void setPtIdx(final Integer ptIdx) {
        this.ptIdx = ptIdx;
    }

    public Integer getScriptType() {
        return scriptType;
    }

    public void setScriptType(final Integer scriptType) {
        this.scriptType = scriptType;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(final String addressType) {
        this.addressType = addressType;
    }

    public Integer getPointer() {
        return pointer;
    }

    public void setPointer(final Integer pointer) {
        this.pointer = pointer;
    }

    public Integer getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(final Integer blockHeight) {
        this.blockHeight = blockHeight;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Date createdAt) {
        this.createdAt = createdAt;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(final Long fee) {
        this.fee = fee;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(final String memo) {
        this.memo = memo;
    }

    public Integer getSubaccount() {
        return subaccount;
    }

    public void setSubaccount(final Integer subaccount) {
        this.subaccount = subaccount;
    }

    public List<Integer> getSubaccounts() {
        return this.subaccounts;
    }

    public void setSubaccounts(final List<Integer> subaccounts) {
        this.subaccounts = subaccounts;
    }

    public String getTxhash() {
        return txhash;
    }

    public void setTxhash(final String txhash) {
        this.txhash = txhash;
    }

    public Long getSatoshi() {
        return satoshi;
    }

    public void setSatoshi(final Long satoshi) {
        this.satoshi = satoshi;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public Long getGaAssetId() {
        return gaAssetId;
    }

    public void setGaAssetId(final Long gaAssetId) {
        this.gaAssetId = gaAssetId;
    }

    public Long getSubtype() {
        return subtype;
    }

    public void setSubtype(final Long subtype) {
        this.subtype = subtype;
    }


}
