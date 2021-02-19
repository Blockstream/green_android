package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TwoFactorConfigData extends JSONData {
    private List<String> allMethods;
    private List<String> enabledMethods;
    private boolean anyEnabled;
    private TwoFactorDetailData email;
    private TwoFactorDetailData sms;
    private TwoFactorDetailData gauth;
    private TwoFactorDetailData phone;
    private ObjectNode limits;
    private ObjectNode twofactorReset;

    @JsonIgnore
    public boolean isTwoFactorResetActive() {
        return twofactorReset != null && twofactorReset.get("is_active").asBoolean();
    }

    @JsonIgnore
    public boolean isTwoFactorResetDisputed() {
        return twofactorReset != null && twofactorReset.get("is_disputed").asBoolean();
    }

    @JsonIgnore
    public Integer getTwoFactorResetDaysRemaining() {
        return null;
    }

    @JsonIgnore
    public TwoFactorDetailData getMethod(final String method) {
        switch (method) {
        case "email":
            return getEmail();
        case "phone":
            return getPhone();
        case "sms":
            return getSms();
        case "gauth":
            return getGauth();
        default:
            return null;
        }
    }

    public ObjectNode getLimits() {
        return limits;
    }

    public void setLimits(final ObjectNode limitsData) {
        this.limits = limitsData;
    }

    public List<String> getAllMethods() {
        return allMethods;
    }

    public void setAllMethods(final List<String> allMethods) {
        this.allMethods = allMethods;
    }

    public List<String> getEnabledMethods() {
        return enabledMethods;
    }

    public void setEnabledMethods(final List<String> enabledMethods) {
        this.enabledMethods = enabledMethods;
    }

    public boolean isAnyEnabled() {
        return anyEnabled;
    }

    public void setAnyEnabled(final boolean anyEnabled) {
        this.anyEnabled = anyEnabled;
    }

    public TwoFactorDetailData getEmail() {
        return email;
    }

    public void setEmail(final TwoFactorDetailData email) {
        this.email = email;
    }

    public TwoFactorDetailData getSms() {
        return sms;
    }

    public void setSms(final TwoFactorDetailData sms) {
        this.sms = sms;
    }

    public TwoFactorDetailData getGauth() {
        return gauth;
    }

    public void setGauth(final TwoFactorDetailData gauth) {
        this.gauth = gauth;
    }

    public TwoFactorDetailData getPhone() {
        return phone;
    }

    public void setPhone(final TwoFactorDetailData phone) {
        this.phone = phone;
    }

    public ObjectNode getTwofactorReset() {
        return twofactorReset;
    }

    public void setTwofactorReset(final ObjectNode twofactorReset) {
        this.twofactorReset = twofactorReset;
    }
}

