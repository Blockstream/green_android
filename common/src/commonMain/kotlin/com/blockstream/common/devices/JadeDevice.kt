package com.blockstream.common.devices

import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.HardwareConnectInteraction
import com.blockstream.common.jade.JadeHWWallet
import com.blockstream.jade.JadeAPI
import com.blockstream.jade.data.JadeNetworks

interface JadeDevice : JadeDeviceApi, GreenDevice

interface JadeDeviceApi : DeviceOperatingNetwork {
    var jadeApi: JadeAPI?
}

class JadeDeviceApiImpl: JadeDeviceApi {
    override var jadeApi: JadeAPI? = null

    override suspend fun getOperatingNetworkForEnviroment(greenDevice: GreenDevice, gdk: Gdk, isTestnet: Boolean): Network {
        return (greenDevice.gdkHardwareWallet as? JadeHWWallet)?.let { jadeHWWallet ->
            jadeHWWallet.getVersionInfo().jadeNetworks.let { networks ->
                when (networks) {
                    JadeNetworks.MAIN -> {
                        gdk.networks().bitcoinElectrum.takeIf { !isTestnet }
                    }
                    JadeNetworks.TEST -> {
                        gdk.networks().testnetBitcoinElectrum.takeIf { isTestnet  }
                    }
                    else -> {
                        if(isTestnet) gdk.networks().testnetBitcoinElectrum else gdk.networks().bitcoinElectrum
                    }
                }
            }
        } ?: throw Exception("Not HWWallet initiated")
    }

    override suspend fun getOperatingNetwork(greenDevice: GreenDevice, gdk: Gdk, interaction: HardwareConnectInteraction): Network {
        return (greenDevice.gdkHardwareWallet as? JadeHWWallet)?.let { jadeHWWallet ->
            jadeHWWallet.getVersionInfo().jadeNetworks.let { networks->
                when (networks) {
                    JadeNetworks.MAIN -> {
                        gdk.networks().bitcoinElectrum
                    }
                    JadeNetworks.TEST -> {
                        gdk.networks().testnetBitcoinElectrum
                    }
                    else -> {
                        interaction.requestNetwork()
                    }
                }
            }
        } ?: throw Exception("Not HWWallet initiated")
    }
}