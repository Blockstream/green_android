package com.greenaddress.jade.entities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class TxInput {
    final boolean isWitness;
    final List<Long> path;

    TxInput(final boolean isWitness, final List<Long> path) {

        this.isWitness = isWitness;
        this.path = path;
    }

    @JsonGetter("is_witness")
    public boolean getIsWitness() {
        return isWitness;
    }

    @JsonGetter("path")
    public List<Long> getPath() {
        return path;
    }
}
