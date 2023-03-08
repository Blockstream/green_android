package com.greenaddress.greenapi

import com.blockstream.DeviceBrand
import kotlinx.coroutines.CompletableDeferred

interface HWWalletBridge {
    fun interactionRequest(hw: HWWallet?, completable: CompletableDeferred<Boolean>?, text: String?)
    fun requestPinMatrix(deviceBrand: DeviceBrand?): String?
    fun requestPassphrase(deviceBrand: DeviceBrand?): String?
}