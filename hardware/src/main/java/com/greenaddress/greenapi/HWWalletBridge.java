package com.greenaddress.greenapi;

import androidx.annotation.Nullable;

import com.blockstream.DeviceBrand;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public interface HWWalletBridge {
    void interactionRequest(final HWWallet hw, @Nullable final Completable completable, @Nullable final String text);

    Single<String> requestPinMatrix(DeviceBrand deviceBrand);
    Single<String> requestPassphrase(DeviceBrand deviceBrand);
}
