package com.blockstream

import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.data.Device
import com.blockstream.jade.JadeAPI
import com.blockstream.jade.data.JadeNetworks
import com.blockstream.jade.data.JadeState
import com.blockstream.jade.data.VersionInfo
import com.blockstream.jade.entities.JadeError
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import com.greenaddress.greenbits.wallets.JadeHWWalletJava

class JadeHWWallet constructor(
    val gdk: Gdk,
    val jadeApi: JadeAPI,
    device: Device,
    versionInfo: VersionInfo,
    hardwareQATester: HardwareQATester
) : JadeHWWalletJava(gdk, jadeApi, device, versionInfo, hardwareQATester) {

    fun getVersionInfo(): VersionInfo {
        return jadeApi.versionInfo
    }

    val isMainnet: Boolean
        get() = getVersionInfo().jadeNetworks.let { it == JadeNetworks.MAIN || it == JadeNetworks.ALL }

    val isUninitialized: Boolean
        get() = getVersionInfo().jadeState.let { it == JadeState.UNINIT || it == JadeState.UNSAVED }

    // Authenticate Jade with pinserver and check firmware version with fw-server
    suspend fun authenticate(hwWalletLogin: HwWalletLogin,
                             jadeFirmwareManager: JadeFirmwareManager): Boolean {
        /*
         * 1. authenticate the user (see above)
         * 2. check the firmware (and maybe OTA)
         * 3. authenticate the user *if required* - as we may have OTA'd and rebooted the hww.  Should be a no-op if not needed.
         */

        // authenticate the user (see above)
        authUser(hwWalletLogin)

        // check the firmware (and maybe OTA) for devices that are set-up and are below minimum firmware version (and hence needed unlocking first)
        val fwValid = jadeFirmwareManager.checkFirmware(jade = jadeApi)

        if(fwValid == true){
            return authUser(hwWalletLogin) // re-auth if required
        }else{
            throw JadeError(
                JadeError.UNSUPPORTED_FIRMWARE_VERSION,
                "Insufficient/invalid firmware version", null
            )
        }
    }

    @Synchronized
    fun updateFirmwareVersion(firmwareVersion: String?) {
        firmwareVersion?.also {
            mFirmwareVersion = it
        }
    }
}