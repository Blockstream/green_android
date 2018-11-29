package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SettingsData extends JSONData {
    private Integer altimeout;
    private PricingData pricing;
    private Integer requiredNumBlocks;
    private boolean sound;
    private String unit;

    public Integer getAltimeout() {
        return altimeout;
    }

    public void setAltimeout(Integer altimeout) {
        this.altimeout = altimeout;
    }

    public PricingData getPricing() {
        return pricing;
    }

    public void setPricing(PricingData pricing) {
        this.pricing = pricing;
    }

    public Integer getRequiredNumBlocks() {
        return requiredNumBlocks;
    }

    public void setRequiredNumBlocks(Integer requiredNumBlocks) {
        this.requiredNumBlocks = requiredNumBlocks;
    }

    public boolean isSound() {
        return sound;
    }

    public void setSound(boolean sound) {
        this.sound = sound;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public class PricingData extends JSONData {
        private String currency;
        private String exchange;

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        @Override
        public String toString() {
            return String.format("%s %s", currency, exchange);
        }
    }

    @JsonIgnore
    public ObjectNode toObjectNode() {
        return new ObjectMapper().convertValue(this,ObjectNode.class);
    }
}
