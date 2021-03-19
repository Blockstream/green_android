package com.greenaddress.jade.entities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class TxInput {
    final boolean isWitness;
    private final byte[] script;
    final List<Long> path;
    private final byte[] aeHostCommitment;
    private final byte[] aeHostEntropy;

    TxInput(final boolean isWitness, final byte[] script, final List<Long> path,
            final byte[] aeHostCommitment, final byte[] aeHostEntropy) {

        this.isWitness = isWitness;
        this.script = script;
        this.path = path;
        this.aeHostCommitment = aeHostCommitment;
        this.aeHostEntropy = aeHostEntropy;
    }

    @JsonGetter("is_witness")
    public boolean getIsWitness() {
        return isWitness;
    }

    @JsonGetter("script")
    public byte[] getScript() {
        return script;
    }

    @JsonGetter("path")
    public List<Long> getPath() {
        return path;
    }

    @JsonGetter("ae_host_commitment")
    public byte[] getAeHostCommitment() {
        return aeHostCommitment;
    }

    // Note: the host entropy is not automatically sent with the input data
    // as it is explicitly set in a subsequent message.
    @JsonIgnore
    public byte[] getAeHostEntropy() {
        return aeHostEntropy;
    }
}
