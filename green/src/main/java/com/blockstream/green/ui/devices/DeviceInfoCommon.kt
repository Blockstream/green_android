package com.blockstream.green.ui.devices

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.devices.Device
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.items.NetworkSmallListItem
import com.blockstream.green.ui.items.TitleExpandableListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.utils.isDevelopmentFlavor
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension

interface DeviceInfoCommon{

    fun createNetworkAdapter(
        context: Context,
        viewLifecycleOwner: LifecycleOwner,
        device: Device?,
        greenWallet: GreenWallet,
        settingsManager: SettingsManager
    ): FastItemAdapter<GenericItem> {
        val fastItemAdapter = FastItemAdapter<GenericItem>()
        fastItemAdapter.getExpandableExtension()

        // Listen for app settings changes to enable/disable testnet networks
        settingsManager.getApplicationSettingsLiveData().observe(viewLifecycleOwner) { applicationSettings ->
            fastItemAdapter.clear()

            fastItemAdapter.add(
                TitleListItem(
                    com.blockstream.green.utils.StringHolder(greenWallet.networks.bitcoinGreen.canonicalName),
                    withTopPadding = false,
                )
            )

            fastItemAdapter.add(NetworkSmallListItem(Network.ElectrumMainnet, greenWallet.networks.bitcoinElectrum.productName))
            fastItemAdapter.add(NetworkSmallListItem(Network.GreenMainnet, greenWallet.networks.bitcoinGreen.productName))

            if(device?.supportsLiquid == true){
                fastItemAdapter.add(
                    TitleListItem(
                        com.blockstream.green.utils.StringHolder(greenWallet.networks.liquidGreen.canonicalName),
                        withTopPadding = false,
                    )
                )

                // Only Singlesig Bitcoin is enabled in production
                if(isDevelopmentFlavor) {
                    fastItemAdapter.add(
                        NetworkSmallListItem(
                            Network.ElectrumLiquid,
                            greenWallet.networks.liquidElectrum.productName + " Dev"
                        )
                    )
                }
                fastItemAdapter.add(NetworkSmallListItem(Network.GreenLiquid, greenWallet.networks.liquidGreen.productName))
            }

            if(applicationSettings.testnet) {
                val expandable = TitleExpandableListItem(com.blockstream.green.utils.StringHolder(R.string.id_additional_networks))

                expandable.subItems.add(
                    NetworkSmallListItem(
                        Network.ElectrumTestnet,
                        greenWallet.networks.testnetElectrum.productName
                    )
                )

                expandable.subItems.add(
                    NetworkSmallListItem(
                        Network.GreenTestnet,
                        greenWallet.networks.testnetGreen.productName
                    )
                )

                if(device?.supportsLiquid == true){

                    // Only Singlesig Bitcoin is enabled in production
                    if(isDevelopmentFlavor) {
                        expandable.subItems.add(
                            NetworkSmallListItem(
                                Network.ElectrumTestnetLiquid,
                                greenWallet.networks.testnetLiquidElectrum.productName + " Dev"
                            )
                        )
                    }

                    expandable.subItems.add(
                        NetworkSmallListItem(
                            Network.GreenTestnetLiquid,
                            greenWallet.networks.testnetLiquidGreen.productName
                        )
                    )
                }

                greenWallet.networks.customNetwork?.let {
                    expandable.subItems.add(
                        NetworkSmallListItem(
                            it.id,
                            it.name
                        )
                    )
                }

                fastItemAdapter.add(expandable)
            }
        }

        return fastItemAdapter
    }
}