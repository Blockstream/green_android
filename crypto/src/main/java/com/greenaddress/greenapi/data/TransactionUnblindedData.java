package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.io.Serializable;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionUnblindedData extends JSONData implements Serializable {
    private String txid;
    private String type;
    private List<InputUnblindedData> inputs;
    private List<OutputUnblindedData> outputs;
    private Integer version;

    public String getTxid() {
        return txid;
    }

    public void setTxid(final String txid) {
        this.txid = txid;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public List<InputUnblindedData> getInputs() {
        return inputs;
    }

    public void setInputs(final List<InputUnblindedData> inputs) {
        this.inputs = inputs;
    }

    public List<OutputUnblindedData> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<OutputUnblindedData> outputs) {
        this.outputs = outputs;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}