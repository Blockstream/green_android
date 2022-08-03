package com.blockstream.green.data

import android.os.Parcelable
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.Network
import com.blockstream.green.database.Wallet
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnboardingOptions constructor(
    val isRestoreFlow: Boolean,
    val isWatchOnly: Boolean = false,
    val isTestnet: Boolean? = null,
    val isSinglesig: Boolean? = null,
    val networkType: String? = null,
    val network: Network? = null,
    val walletName: String? = null
) : Parcelable{
    fun createCopyForNetwork(gdkBridge: GdkBridge, networkType: String, isElectrum: Boolean): OnboardingOptions {
        val id = gdkBridge.networks.getNetworkByType(networkTypeOrId = networkType, isElectrum = isElectrum).id
        return copy(network = gdkBridge.networks.getNetworkById(id), networkType = networkType, isSinglesig = isElectrum)
    }

    companion object {
        fun fromWallet(wallet: Wallet) = OnboardingOptions(
            isRestoreFlow = true,
            isWatchOnly = false,
            isTestnet = wallet.isTestnet,
            walletName = wallet.name
        )
    }
}