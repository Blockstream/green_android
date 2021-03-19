package com.greenaddress.jade.entities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TxInputLiquid extends TxInput {
    private final byte[] valueCommitment;

    public TxInputLiquid(final boolean isWitness, final byte[] script, final byte[] valueCommitment,
                         final List<Long> path, final byte[] aeHostCommitment, final byte[] aeHostEntropy) {
        super(isWitness, script, path, aeHostCommitment, aeHostEntropy);
        this.valueCommitment = valueCommitment;
    }

    @JsonGetter("value_commitment")
    public byte[] getValueCommitment() {
        return valueCommitment;
    }
}
