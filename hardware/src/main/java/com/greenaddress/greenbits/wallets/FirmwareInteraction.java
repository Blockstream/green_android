package com.greenaddress.greenbits.wallets;

import androidx.arch.core.util.Function;

public interface FirmwareInteraction {
    void jadeAskForFirmwareUpgrade(String version, boolean isUpgradeRequired, Function<Boolean, Void> callback);
}
