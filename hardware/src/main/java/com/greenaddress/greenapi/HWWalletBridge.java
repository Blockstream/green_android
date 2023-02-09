package com.greenaddress.greenapi;

import androidx.annotation.Nullable;

import com.blockstream.DeviceBrand;
import com.blockstream.gdk.data.Network;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public interface HWWalletBridge {
    void interactionRequest(final HWWallet hw, @Nullable final Completable completable, @Nullable final String text);

    String requestPinMatrix(DeviceBrand deviceBrand);
    String requestPassphrase(DeviceBrand deviceBrand);
}