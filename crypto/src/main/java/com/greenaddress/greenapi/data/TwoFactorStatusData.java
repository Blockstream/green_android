package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TwoFactorStatusData extends JSONData {
    private String status;
    private String action;
    private List<String> methods;
    private String method;
    private Integer attemptsRemaining;
    private String debug;
    private String error;
    private ObjectNode result;
    private HWDeviceDetailData device;
    private HWDeviceRequiredData required_data; // FIXME: Strongly type this

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(final List<String> methods) {
        this.methods = methods;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public Integer getAttemptsRemaining() {
        return attemptsRemaining;
    }

    public void setAttemptsRemaining(final Integer attemptsRemaining) {
        this.attemptsRemaining = attemptsRemaining;
    }

    public String getDebug() {
        return debug;
    }

    public void setDebug(final String debug) {
        this.debug = debug;
    }

    public ObjectNode getResult() {
        return result;
    }

    public void setResult(final ObjectNode result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public HWDeviceDetailData getDevice() {
        return device;
    }

    public void setDevice(final HWDeviceDetailData device) {
        this.device = device;
    }

    public HWDeviceRequiredData getRequiredData() {
        return required_data;
    }

    public void setRequiredData(final HWDeviceRequiredData required_data) {
        this.required_data = required_data;
    }
}
