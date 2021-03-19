package com.greenaddress.greenapi;

import android.content.res.Resources;

import androidx.arch.core.util.Function;

public interface HWWalletBridge {
    void interactionRequest(final HWWallet hw);
    String pinMatrixRequest(final HWWallet hw);
    String passphraseRequest(final HWWallet hw);

    // TODO remove dependency on Resources
    Resources getResources();

    void jadeAskForFirmwareUpgrade(String version, boolean isUpgradeRequired, Function<Boolean, Void> callback);
}
