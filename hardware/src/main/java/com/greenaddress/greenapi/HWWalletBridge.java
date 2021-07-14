package com.greenaddress.greenapi;

import com.blockstream.DeviceBrand;

import io.reactivex.Single;

public interface HWWalletBridge {
    void interactionRequest(final HWWallet hw);

    Single<String> requestPinMatrix(DeviceBrand deviceBrand);
    Single<String> requestPassphrase(DeviceBrand deviceBrand);
}
