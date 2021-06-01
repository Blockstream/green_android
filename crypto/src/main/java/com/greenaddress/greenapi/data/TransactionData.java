package com.greenaddress.greenapi.data;

import android.text.TextUtils;
import android.view.inputmethod.InputBinding;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import org.bitcoinj.core.Sha256Hash;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionData extends JSONData implements Serializable {
    private Integer blockHeight;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
    private Long fee;
    private Long feeRate;
    private String memo;
    private Integer subaccount;
    private List<Integer> subaccounts;
    private String txhash;
    private Map<String,Long> satoshi;
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

    public enum TYPE {
        OUT,
        IN,
        REDEPOSIT
    }

    @JsonIgnore
    public TYPE getTxType() {
        switch (getType()) {
        case "outgoing":
            return TYPE.OUT;
        case "incoming":
            return TYPE.IN;
        case "redeposit":
            return TYPE.REDEPOSIT;
        default:
            return TYPE.OUT;
        }
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

    public Map<String, Long> getSatoshi() {
        return satoshi;
    }

    @JsonIgnore
    public boolean isAsset(String policyAsset) {
        return getFirstAsset(policyAsset) != null;
    }

    @JsonIgnore
    public String getFirstAsset(String policyAsset) {
        // unblinding failed
        if (satoshi == null) {
            return null;
        }

        final Iterator<String> iter = satoshi.keySet().iterator();
        while (iter.hasNext()) {
            final String current = iter.next();
            if (!policyAsset.equals(current))
                return current;
        }
        return null;
    }

    public void setSatoshi(Map<String, Long> satoshi) {
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

    @JsonIgnore
    public String getLocalizedTime(final int style) {
        return DateFormat.getTimeInstance(style).format(createdAt);
    }

    @JsonIgnore
    public String getLocalizedDate(final int style) {
        return DateFormat.getDateInstance(style).format(createdAt);
    }

    @JsonIgnore
    public boolean isSpent() {
        for (final InputOutputData output : getOutputs()) {
            if (output.getIsSpent()) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public int getConfirmations(final int currentBlock) {
        if (blockHeight == 0)
            return 0;
        if (blockHeight != null)
            return currentBlock - blockHeight + 1;
        return 0;
    }

    @JsonIgnore
    public boolean hasEnoughConfirmations(final int currentBlock) {
        return getConfirmations(currentBlock) >= 6;
    }

    public String getUnblindedString() {
        final List<String> inputs_outputs_list = new ArrayList<>();
        for (InputOutputData e: this.getInputs()) {
            final String s = e.getUnblindedString();
            if (!s.isEmpty()) {
                inputs_outputs_list.add(s);
            }
        }
        for (InputOutputData e: this.getOutputs()) {
            final String s = e.getUnblindedString();
            if (!s.isEmpty()) {
                inputs_outputs_list.add(s);
            }
        }
        return TextUtils.join(",", inputs_outputs_list);
    }

    public TransactionUnblindedData getUnblindedData() {
        final TransactionUnblindedData tx = new TransactionUnblindedData();
        tx.setVersion(0);
        tx.setTxid(getTxhash());
        tx.setType(getType());
        tx.setInputs(new ArrayList<>());
        tx.setOutputs(new ArrayList<>());
        for (final InputOutputData e: getInputs()) {
            if (e.hasUnblindingData()) {
                final InputUnblindedData input = new InputUnblindedData(e.getPtIdx(), e.getAssetId(), e.getAssetblinder(), e.getSatoshi(), e.getAmountblinder());
                tx.getInputs().add(input);
            }
        }
        for (final InputOutputData e: getOutputs()) {
            if (e.hasUnblindingData()) {
                final OutputUnblindedData output = new OutputUnblindedData(e.getPtIdx(), e.getAssetId(), e.getAssetblinder(), e.getSatoshi(), e.getAmountblinder());
                tx.getOutputs().add(output);
            }
        }
        return tx;
    }
}
