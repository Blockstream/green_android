package com.blockstream.green.devices

import android.content.Context
import com.blockstream.DeviceBrand
import com.blockstream.gdk.data.Network
import com.blockstream.green.gdk.GdkSession
import com.greenaddress.greenapi.HWWalletBridge
import com.greenaddress.greenbits.wallets.FirmwareInteraction
import kotlinx.coroutines.CompletableDeferred

interface HardwareConnectInteraction : FirmwareInteraction, HWWalletBridge {
    fun context(): Context
    fun showInstructions(resId: Int)
    fun getGreenSession(): GdkSession

    fun onJadeInitialization(session: GdkSession)
    fun showError(err: String)
    fun getConnectionNetwork(): Network

    fun onDeviceReady()
    fun onDeviceFailed()

    fun requestPin(deviceBrand: DeviceBrand): CompletableDeferred<String>
    fun requestPinBlocking(deviceBrand: DeviceBrand): String
}