package com.greenaddress.greenapi;

import com.blockstream.HardwareQATester;
import com.blockstream.common.gdk.Gdk;
import com.blockstream.common.gdk.device.GdkHardwareWallet;
import com.blockstream.common.gdk.data.Device;

import javax.annotation.Nullable;

public abstract class HWWallet extends GdkHardwareWallet {
    protected Gdk mGdk;
    protected Device mDevice;
    protected HardwareQATester mHardwareQATester;
    protected String mFirmwareVersion;
    protected String mModel;

    public Device getDevice() {
        return mDevice;
    }

    @Nullable
    public HardwareQATester getHardwareEmulator() {
        return mHardwareQATester;
    }

    @Nullable
    public String getFirmwareVersion() {
        return mFirmwareVersion;
    }

    public String getModel(){
        return mModel;
    }
}
