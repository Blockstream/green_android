package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class HWDeviceData extends JSONData {
    public enum HWDeviceDataLiquidSupport {
        None,
        Lite,
        Full;

        @JsonValue
        public int toValue() {
            return ordinal();
        }
    }

    private HWDeviceDetailData device;

    public HWDeviceData() {}

    public HWDeviceData(final String name, final boolean supportsLowR, final boolean supportsArbitraryScripts,
                        final HWDeviceDataLiquidSupport supportsLiquid) {
        device = new HWDeviceDetailData(name, supportsLowR, supportsArbitraryScripts, supportsLiquid);
    }

    public HWDeviceDetailData getDevice() {
        return device;
    }

    public void setDevice(final HWDeviceDetailData device) {
        this.device = device;
    }
}
