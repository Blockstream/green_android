package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import org.bitcoinj.core.Coin;

import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTransactionData extends JSONData {
    private List<BalanceData> addressees;
    private List<TransactionData> utxos;
    private Long feeRate;
    private Boolean sendAll;
    private Long changeIndex;
    private Boolean haveChange;
    private String transaction;
    private String error;
    private Long availableTotal;
    private Long fee;
    private Long value;
    private Long changeAmount;
    private InputOutputData changeAddress;
    private Integer changeSubaccount;
    private Long satoshi;
    private Boolean serverSigned;
    private Integer transactionSize;
    private Integer transactionVsize;
    private Integer transactionWeight;
    private Boolean twofactorRequired;
    private Boolean twofactorUnderLimit;
    private List<Integer> usedUtxos;
    private Boolean userSigned;
    private String utxoStrategy;
    private String txhash;
    private Boolean addresseesReadOnly;
    private Integer feeIncrement;
    private Boolean isRedeposit;
    private Boolean isSweep;
    private Integer networkFee;
    private String memo;

    @JsonIgnore
    public Coin getFeeAsCoin() {
        return fee == null ? null : Coin.valueOf(fee);
    }

    @JsonIgnore
    public Coin getAmountAsCoin() {
        return satoshi == null ? null : Coin.valueOf(satoshi);
    }

    @JsonIgnore
    public Coin getAvailableTotalAsCoin() {
        return availableTotal == null ? null : Coin.valueOf(availableTotal);
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(final String memo) {
        this.memo = memo;
    }

    public Boolean getAddresseesReadOnly() {
        return addresseesReadOnly;
    }

    public void setAddresseesReadOnly(final Boolean addresseesReadOnly) {
        this.addresseesReadOnly = addresseesReadOnly;
    }

    public Integer getFeeIncrement() {
        return feeIncrement;
    }

    public void setFeeIncrement(final Integer feeIncrement) {
        this.feeIncrement = feeIncrement;
    }

    public Boolean getIsRedeposit() {
        return isRedeposit;
    }

    public void setIsRedeposit(final Boolean redeposit) {
        isRedeposit = redeposit;
    }

    public Boolean getIsSweep() {
        return isSweep;
    }

    public void setIsSweep(final Boolean sweep) {
        isSweep = sweep;
    }

    public Integer getNetworkFee() {
        return networkFee;
    }

    public void setNetworkFee(final Integer networkFee) {
        this.networkFee = networkFee;
    }

    public Long getSatoshi() {
        return satoshi;
    }

    public void setSatoshi(final Long satoshi) {
        this.satoshi = satoshi;
    }

    public Boolean getServerSigned() {
        return serverSigned;
    }

    public void setServerSigned(final Boolean serverSigned) {
        this.serverSigned = serverSigned;
    }

    public Integer getTransactionSize() {
        return transactionSize;
    }

    public void setTransactionSize(final Integer transactionSize) {
        this.transactionSize = transactionSize;
    }

    public Integer getTransactionVsize() {
        return transactionVsize;
    }

    public void setTransactionVsize(final Integer transactionVsize) {
        this.transactionVsize = transactionVsize;
    }

    public Integer getTransactionWeight() {
        return transactionWeight;
    }

    public void setTransactionWeight(final Integer transactionWeight) {
        this.transactionWeight = transactionWeight;
    }

    public Boolean getTwofactorRequired() {
        return twofactorRequired;
    }

    public void setTwofactorRequired(final Boolean twofactorRequired) {
        this.twofactorRequired = twofactorRequired;
    }

    public Boolean getTwofactorUnderLimit() {
        return twofactorUnderLimit;
    }

    public void setTwofactorUnderLimit(final Boolean twofactorUnderLimit) {
        this.twofactorUnderLimit = twofactorUnderLimit;
    }

    public List<Integer> getUsedUtxos() {
        return usedUtxos;
    }

    public void setUsedUtxos(final List<Integer> usedUtxos) {
        this.usedUtxos = usedUtxos;
    }

    public Boolean getUserSigned() {
        return userSigned;
    }

    public void setUserSigned(final Boolean userSigned) {
        this.userSigned = userSigned;
    }

    public String getUtxoStrategy() {
        return utxoStrategy;
    }

    public void setUtxoStrategy(final String utxoStrategy) {
        this.utxoStrategy = utxoStrategy;
    }

    public List<BalanceData> getAddressees() {
        return addressees;
    }

    public void setAddressees(final List<BalanceData> addressees) {
        this.addressees = addressees;
    }

    public List<TransactionData> getUtxos() {
        return utxos;
    }

    public void setUtxos(final List<TransactionData> utxos) {
        this.utxos = utxos;
    }

    public Long getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(final Long feeRate) {
        this.feeRate = feeRate;
    }

    public Boolean getSendAll() {
        return sendAll;
    }

    public void setSendAll(final Boolean sendAll) {
        this.sendAll = sendAll;
    }

    public Long getChangeIndex() {
        return changeIndex;
    }

    public void setChangeIndex(final Long changeIndex) {
        this.changeIndex = changeIndex;
    }

    public Boolean getHaveChange() {
        return haveChange;
    }

    public void setHaveChange(final Boolean haveChange) {
        this.haveChange = haveChange;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(final String transaction) {
        this.transaction = transaction;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public Long getAvailableTotal() {
        return availableTotal;
    }

    public void setAvailableTotal(final Long availableTotal) {
        this.availableTotal = availableTotal;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(final Long fee) {
        this.fee = fee;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(final Long value) {
        this.value = value;
    }

    public Long getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(final Long changeAmount) {
        this.changeAmount = changeAmount;
    }

    public InputOutputData getChangeAddress() {
        return changeAddress;
    }

    public void setChangeAddress(final InputOutputData changeAddress) {
        this.changeAddress = changeAddress;
    }

    public Integer getChangeSubaccount() {
        return changeSubaccount;
    }

    public void setChangeSubaccount(final Integer changeSubaccount) {
        this.changeSubaccount = changeSubaccount;
    }

    public String getTxhash() {
        return txhash;
    }

    public void setTxhash(final String txhash) {
        this.txhash = txhash;
    }



}
