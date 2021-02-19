package com.greenaddress.greenapi;

import android.app.Activity;
import android.content.res.Resources;

public interface HWWalletBridge {
    public void interactionRequest(final HWWallet hw);
    public String pinMatrixRequest(final HWWallet hw);
    public String passphraseRequest(final HWWallet hw);

    // TODO remove dependency on runOnUiThread
    public void runOnUiThread(Runnable action);

    // TODO remove dependency on Resources
    public Resources getResources();

    // TODO remove dependency on Activity
    public Activity getActivity();
}
