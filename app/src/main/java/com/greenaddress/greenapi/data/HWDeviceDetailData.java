package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class HWDeviceDetailData extends JSONData {
    private String name;
    private boolean supportsLowR;
    private boolean supportsArbitraryScripts;

    // Ctor for serialisation only
    public HWDeviceDetailData() {
        this.name = null;
        this.supportsLowR = false;
        this.supportsArbitraryScripts = false;
    }

    public HWDeviceDetailData(final String name, final boolean supportsLowR, final boolean supportsArbitraryScripts) {
        this.name = name;
        this.supportsLowR = supportsLowR;
        this.supportsArbitraryScripts = supportsArbitraryScripts;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isSupportsArbitraryScripts() {
        return supportsArbitraryScripts;
    }

    public void setSupportsArbitraryScripts(final boolean supportsArbitraryScripts) {
        this.supportsArbitraryScripts = supportsArbitraryScripts;
    }

    public boolean isSupportsLowR() {
        return supportsLowR;
    }

    public void setSupportsLowR(final boolean supportsLowR) {
        this.supportsLowR = supportsLowR;
    }
}
