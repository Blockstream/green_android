package com.blockstream.green.data

import android.os.Parcelable
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.green.database.Wallet
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnboardingOptions constructor(
    val isRestoreFlow: Boolean,
    val isWatchOnly: Boolean = false,
    val isSingleSig: Boolean? = null,
    val networkType: String? = null,
    val network: Network? = null,
    val walletName: String? = null
) : Parcelable{
    fun createCopyForNetwork(greenWallet: GreenWallet, networkType: String, isElectrum: Boolean): OnboardingOptions {
        val id = greenWallet.networks.getNetworkByType(networkTypeOrId = networkType, isElectrum = isElectrum).id
        return copy(network = greenWallet.networks.getNetworkById(id), networkType = networkType)
    }

    companion object {
        fun fromWallet(wallet: Wallet, network: Network) = OnboardingOptions(
            isRestoreFlow = true,
            isWatchOnly = false,
            networkType = wallet.network,
            network = network,
            walletName = wallet.name
        )
    }
}