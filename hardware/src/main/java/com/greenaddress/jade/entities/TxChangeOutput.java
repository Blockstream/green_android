package com.greenaddress.jade.entities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TxChangeOutput {
    private final List<Long> path;
    private final String recoveryxpub;
    private final Integer csvBlocks;
    private final String variant;

    public TxChangeOutput(final List<Long> path, final String recoveryxpub, final Integer csvBlocks, final String variant) {
        // Convert path to strings
        this.path = path;
        this.recoveryxpub = recoveryxpub;
        this.csvBlocks = csvBlocks;
        this.variant = variant;
    }

    @JsonGetter("path")
    public List<Long> getPath() {
        return path;
    }

    @JsonGetter("recovery_xpub")
    public String getRecoveryXpub() {
        return recoveryxpub;
    }

    @JsonGetter("csv_blocks")
    public Integer getCsvBlocks() {
        return csvBlocks;
    }

    @JsonGetter("variant")
    public String getVariant() {
        return variant;
    }
}
