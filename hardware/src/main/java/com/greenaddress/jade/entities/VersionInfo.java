package com.greenaddress.jade.entities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown=true)
public class VersionInfo {
    private String jadeVersion;
    private Integer jadeOtaMaxChunk;
    private String jadeConfig;
    private String boardType;
    private String jadeFeatures;
    private String idfVersion;
    private String chipFeatures;
    private String efusemac;
    private String jadeState;
    private String jadeNetworks;
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

    @JsonGetter("BOARD_TYPE")
    public String getBoardType() {
        return boardType;
    }

    @JsonSetter("BOARD_TYPE")
    public void setBoardType(String boardType) {
        this.boardType = boardType;
    }

    @JsonGetter("JADE_FEATURES")
    public String getJadeFeatures() {
        return jadeFeatures;
    }

    @JsonSetter("JADE_FEATURES")
    public void setJadeFeatures(String jadeFeatures) {
        this.jadeFeatures = jadeFeatures;
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

    @JsonGetter("JADE_STATE")
    public String getJadeState() {
        return jadeState;
    }

    @JsonSetter("JADE_STATE")
    public void setJadeState(String jadeState) {
        this.jadeState = jadeState;
    }

    @JsonGetter("JADE_NETWORKS")
    public String getJadeNetworks() {
        return jadeNetworks;
    }

    @JsonSetter("JADE_NETWORKS")
    public void setJadeNetworks(String jadeNetworks) {
        this.jadeNetworks = jadeNetworks;
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
