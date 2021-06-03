package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Deprecated
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class HWDeviceDetailData extends JSONData {
    private String name;
    private boolean supportsLowR;
    private boolean supportsArbitraryScripts;
    private HWDeviceData.HWDeviceDataLiquidSupport supportsLiquid;
    private HWDeviceData.HWDeviceAntiExfilSupport supportsAeProtocol;

    // Ctor for serialisation only
    public HWDeviceDetailData() {
        this.name = null;
        this.supportsLowR = false;
        this.supportsArbitraryScripts = false;
        this.supportsLiquid = HWDeviceData.HWDeviceDataLiquidSupport.None;
        this.supportsAeProtocol = HWDeviceData.HWDeviceAntiExfilSupport.None;
    }

    public HWDeviceDetailData(final String name, final boolean supportsLowR, final boolean supportsArbitraryScripts,
                              final HWDeviceData.HWDeviceDataLiquidSupport supportsLiquid,
                              final HWDeviceData.HWDeviceAntiExfilSupport aeProtocolSupportLevel) {
        this.name = name;
        this.supportsLowR = supportsLowR;
        this.supportsArbitraryScripts = supportsArbitraryScripts;
        this.supportsLiquid = supportsLiquid;
        this.supportsAeProtocol = aeProtocolSupportLevel;
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

    public void setSupportsLiquid(final HWDeviceData.HWDeviceDataLiquidSupport supportsLiquid) {
        this.supportsLiquid = supportsLiquid;
    }

    public HWDeviceData.HWDeviceAntiExfilSupport getSupportsAeProtocol() {
        return this.supportsAeProtocol;
    }

    public void setSupportsAeProtocol(final HWDeviceData.HWDeviceAntiExfilSupport supportsAEProtocol) {
        this.supportsAeProtocol = supportsAEProtocol;
    }
}
