package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.Serializable;

import javax.annotation.Nullable;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetInfoData extends JSONData implements Serializable {
    private String assetId;
    private String name;
    private Integer precision;
    private String ticker;
    @Nullable
    private EntityData entity;

    public AssetInfoData() {}

    public AssetInfoData(final String assetId) {
        this.assetId = assetId;
        this.name = assetId;
        this.precision = 0;
        this.ticker = "";
        this.entity = new EntityData("");
    }

    public AssetInfoData(final String assetId, final String name, final Integer precision, final String ticker,
                         final String domain) {
        this.assetId = assetId;
        this.name = name;
        this.precision = precision;
        this.ticker = ticker;
        this.entity = new EntityData(domain);
    }

    @JsonIgnore
    public ObjectNode toObjectNode() {
        return new ObjectMapper().convertValue(this, ObjectNode.class);
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(final String assetId) {
        this.assetId = assetId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(final Integer precision) {
        this.precision = precision;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(final String ticker) {
        this.ticker = ticker;
    }

    @Nullable
    public EntityData getEntity() {
        return entity;
    }

    public void setEntity(final EntityData entity) {
        this.entity = entity;
    }
}
