package com.blockstream.green.devices

import android.content.Context
import com.blockstream.DeviceBrand
import com.blockstream.gdk.data.Network
import com.blockstream.green.gdk.GreenSession
import com.greenaddress.greenapi.HWWalletBridge
import com.greenaddress.greenbits.wallets.FirmwareInteraction
import io.reactivex.rxjava3.core.Single

interface HardwareConnectInteraction : FirmwareInteraction, HWWalletBridge {
    fun context(): Context
    fun showInstructions(resId: Int)
    fun getGreenSession(): GreenSession
    fun showError(error: String)
    fun getConnectionNetwork(): Network
    
    
    fun onDeviceReady()
    fun onDeviceFailed()

    fun requestPin(deviceBrand: DeviceBrand): Single<String>
}