package com.blockstream.green.data

import android.content.Context
import android.os.Parcelable
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.green.database.Wallet
import com.blockstream.green.utils.isProductionFlavor
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnboardingOptions(
    val isRestoreFlow: Boolean,
    val isWatchOnly: Boolean = false,
    val isSingleSig: Boolean = false,
    val networkType: String? = null,
    val network: Network? = null,
    val walletName: String? = null
) : Parcelable{
    fun getNetwork(isElectrum: Boolean, greenWallet: GreenWallet): Network {
        val id = when (networkType) {
            Network.GreenMainnet -> {
                if (isElectrum) Network.ElectrumMainnet else Network.GreenMainnet
            }
            Network.GreenLiquid -> {
                if (isElectrum) Network.ElectrumLiquid else Network.GreenLiquid
            }
            Network.GreenTestnetLiquid -> {
                if (isElectrum) Network.ElectrumTestnetLiquid else Network.GreenTestnetLiquid
            }
            else -> {
                if (isElectrum) Network.ElectrumTestnet else Network.GreenTestnet
            }
        }

        return greenWallet.networks.getNetworkById(id)
    }

    // Singlesig mainnet/liquid is enabled only in Development flavor
    // Singlesig testnet is available in production
    fun isSinglesigNetworkEnabledForBuildFlavor(context: Context): Boolean {
        if(context.isProductionFlavor()){
            return networkType == "testnet" || networkType == "mainnet"
        }
        return true
    }

    fun createCopyForNetwork(greenWallet: GreenWallet, networkType: String, isElectrum: Boolean): OnboardingOptions {
        val id = when (networkType) {
            Network.GreenMainnet -> {
                if (isElectrum) Network.ElectrumMainnet else Network.GreenMainnet
            }
            Network.GreenLiquid -> {
                if (isElectrum) Network.ElectrumLiquid else Network.GreenLiquid
            }
            Network.GreenTestnetLiquid -> {
                if (isElectrum) Network.ElectrumTestnetLiquid else Network.GreenTestnetLiquid
            }
            else -> {
                if (isElectrum) Network.ElectrumTestnet else Network.GreenTestnet
            }
        }

        return copy(network = greenWallet.networks.getNetworkById(id), networkType = networkType)
    }

    companion object{
        fun fromWallet(wallet: Wallet, network: Network): OnboardingOptions {

            return OnboardingOptions(
                isRestoreFlow = true,
                isWatchOnly = false,
                networkType = wallet.network,
                network = network,
                walletName = wallet.name
            )

        }
    }
}