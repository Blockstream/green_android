package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class HWDeviceData extends JSONData {
    private HWDeviceDetailData device;

    public HWDeviceData() {}

    public HWDeviceData(final String name, final boolean supportsLowR, final boolean supportsArbitraryScripts) {
        device = new HWDeviceDetailData(name, supportsLowR, supportsArbitraryScripts);
    }

    public HWDeviceDetailData getDevice() {
        return device;
    }

    public void setDevice(final HWDeviceDetailData device) {
        this.device = device;
    }
}
