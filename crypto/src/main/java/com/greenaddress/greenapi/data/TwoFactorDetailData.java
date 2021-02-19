package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TwoFactorDetailData extends JSONData {
    private boolean confirmed;
    private boolean enabled;
    private String data;

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(final boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }
}
