package com.greenaddress.greenbits.ui.authentication

import android.content.Context
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWalletBridge
import com.greenaddress.greenapi.Session
import com.greenaddress.greenapi.data.NetworkData
import com.greenaddress.greenbits.wallets.FirmwareInteraction

interface HardwareConnectInteraction : FirmwareInteraction, HWWalletBridge {
    fun getContext(): Context
    fun showInstructions(resId: Int)
    fun getSession(): Session
    fun getHwwallet(): HWWallet
    fun setHwwallet(hwWallet: HWWallet)
    fun showError(error: String)
    fun getNetworkData(): NetworkData
    fun onLoginSuccess()
    fun showFirmwareOutdated(onContinue: Runnable, onClose: Runnable)
    fun getPin(): String?
}