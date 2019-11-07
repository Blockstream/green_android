package com.greenaddress.jade.entities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TxInputBtc extends TxInput {

    private final byte[] inputTx;
    private final byte[] script;
    private final Long satoshi;

    public TxInputBtc(final boolean isWitness, final byte[] inputTx, final byte[] script, final Long satoshi,
                      final List<Long> path) {
        super(isWitness, path);
        this.inputTx = inputTx;
        this.script = script;
        this.satoshi = satoshi;
    }

    @JsonGetter("input_tx")
    public byte[] getInputTx() {
        return inputTx;
    }

    @JsonGetter("script")
    public byte[] getScript() {
        return script;
    }

    @JsonGetter("satoshi")
    public Long getSatoshi() {
        return satoshi;
    }
}
