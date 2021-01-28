package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettingsData extends JSONData {

    private Integer altimeout;
    private PricingData pricing;
    private NotificationsData notifications;
    private Integer requiredNumBlocks;
    private boolean sound;
    private String unit;
    private String pgp;
    private Integer csvtime;

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

    public NotificationsData getNotifications() {
        return notifications;
    }

    public void setNotifications(NotificationsData notifications) {
        this.notifications = notifications;
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

    public String getPgp() {
        return pgp;
    }

    public void setPgp(String pgp) {
        this.pgp = pgp;
    }

    public Integer getCsvtime() {
        return csvtime;
    }

    public void setCsvtime(Integer csvtime) {
        this.csvtime = csvtime;
    }

    @JsonIgnore
    public ObjectNode toObjectNode() {
        return new ObjectMapper().convertValue(this,ObjectNode.class);
    }

    @JsonIgnore
    public int getFeeBuckets(final int[] mBlockTargets) {
        for (int i = 0; i < mBlockTargets.length; i++) {
            if (mBlockTargets[i] == getRequiredNumBlocks())
                return i;
        }
        return 1;
    }
}
