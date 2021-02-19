package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class HWDeviceDetailData extends JSONData {
    private String name;
    private boolean supportsLowR;
    private boolean supportsArbitraryScripts;
    private HWDeviceData.HWDeviceDataLiquidSupport supportsLiquid;

    // Ctor for serialisation only
    public HWDeviceDetailData() {
        this.name = null;
        this.supportsLowR = false;
        this.supportsArbitraryScripts = false;
        this.supportsLiquid = HWDeviceData.HWDeviceDataLiquidSupport.None;
    }

    public HWDeviceDetailData(final String name, final boolean supportsLowR, final boolean supportsArbitraryScripts,
                              final HWDeviceData.HWDeviceDataLiquidSupport supportsLiquid) {
        this.name = name;
        this.supportsLowR = supportsLowR;
        this.supportsArbitraryScripts = supportsArbitraryScripts;
        this.supportsLiquid = supportsLiquid;
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

    public HWDeviceData.HWDeviceDataLiquidSupport getSupportsLiquid() {
        return supportsLiquid;
    }

    public void setSupportsLiquid(HWDeviceData.HWDeviceDataLiquidSupport supportsLiquid) {
        this.supportsLiquid = supportsLiquid;
    }
}
