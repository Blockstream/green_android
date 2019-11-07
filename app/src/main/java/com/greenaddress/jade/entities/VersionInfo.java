package com.greenaddress.jade.entities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown=true)
public class VersionInfo {
    private String jadeVersion;
    private Integer jadeOtaMaxChunk;
    private String jadeConfig;
    private String idfVersion;
    private String chipFeatures;
    private String efusemac;
    private Integer jadeFreeHeap;
    private Boolean jadeHasPin;

    @JsonGetter("JADE_VERSION")
    public String getJadeVersion() {
        return jadeVersion;
    }

    @JsonSetter("JADE_VERSION")
    public void setJadeVersion(String jadeVersion) {
        this.jadeVersion = jadeVersion;
    }

    @JsonGetter("JADE_OTA_MAX_CHUNK")
    public Integer getJadeOtaMaxChunk() {
        return jadeOtaMaxChunk;
    }

    @JsonSetter("JADE_OTA_MAX_CHUNK")
    public void setJadeOtaMaxChunk(Integer jadeOtaMaxChunk) {
        this.jadeOtaMaxChunk = jadeOtaMaxChunk;
    }

    @JsonGetter("JADE_CONFIG")
    public String getJadeConfig() {
        return jadeConfig;
    }

    @JsonSetter("JADE_CONFIG")
    public void setJadeConfig(String jadeConfig) {
        this.jadeConfig = jadeConfig;
    }

    @JsonGetter("IDF_VERSION")
    public String getIdfVersion() {
        return idfVersion;
    }

    @JsonSetter("IDF_VERSION")
    public void setIdfVersion(String idfVersion) {
        this.idfVersion = idfVersion;
    }

    @JsonGetter("CHIP_FEATURES")
    public String getChipFeatures() {
        return chipFeatures;
    }

    @JsonSetter("CHIP_FEATURES")
    public void setChipFeatures(String chipFeatures) {
        this.chipFeatures = chipFeatures;
    }

    @JsonGetter("EFUSEMAC")
    public String getEfusemac() {
        return efusemac;
    }

    @JsonSetter("EFUSEMAC")
    public void setEfusemac(String efusemac) {
        this.efusemac = efusemac;
    }

    @JsonGetter("JADE_FREE_HEAP")
    public Integer getJadeFreeHeap() {
        return jadeFreeHeap;
    }

    @JsonSetter("JADE_FREE_HEAP")
    public void setJadeFreeHeap(Integer jadeFreeHeap) {
        this.jadeFreeHeap = jadeFreeHeap;
    }

    @JsonGetter("JADE_HAS_PIN")
    public Boolean getHasPin() {
        return jadeHasPin;
    }

    @JsonSetter("JADE_HAS_PIN")
    public void setJadeHasPin(Boolean jadeHasPin) {
        this.jadeHasPin = jadeHasPin;
    }
}
