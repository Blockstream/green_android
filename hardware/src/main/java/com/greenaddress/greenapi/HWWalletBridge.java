package com.greenaddress.greenapi;

import android.content.res.Resources;

import androidx.arch.core.util.Function;

public interface HWWalletBridge {
    public void interactionRequest(final HWWallet hw);
    public String pinMatrixRequest(final HWWallet hw);
    public String passphraseRequest(final HWWallet hw);

    // TODO remove dependency on Resources
    public Resources getResources();

    public void jadeAskForFirmwareUpgrade(String version, boolean isUpgradeRequired, Function<Boolean, Void> callback);
}
