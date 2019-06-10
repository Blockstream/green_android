package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.Serializable;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetInfoData extends JSONData implements Serializable {
    private String assetId;
    private String name;
    private Integer precision;
    private String ticker;

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
}
