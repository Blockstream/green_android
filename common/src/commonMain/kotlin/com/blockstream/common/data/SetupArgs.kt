package com.blockstream.common.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network


@Parcelize
data class SetupArgs(
    val mnemonic: String = "",
    val password: String? = null,
    val isRestoreFlow: Boolean = false,
    val isWatchOnly: Boolean = false,
    val isTestnet: Boolean? = null,
    val isSinglesig: Boolean? = null,
    val networkType: String? = null,
    val greenWallet: GreenWallet? = null,
    val assetId: String? = null,
    val network: Network? = null,
    val accountType: AccountType? = null,
    val xpub: String? = null,
    val page: Int = 1,
    val isShowRecovery: Boolean = false,
    val isLightning: Boolean = false
) : Parcelable {

    val mnemonicAsWords
        get() = mnemonic.split(" ")

    val isGenerateMnemonic
        get() = mnemonic.isEmpty() && !isShowRecovery

    fun nextPage() = copy(page = page + 1)

    fun pageOne() = copy(page = 1)

    fun isAddAccount() = greenWallet != null && network != null && assetId != null

    fun createCopyForNetwork(gdk: Gdk, networkType: String, isElectrum: Boolean): SetupArgs {
        val id = gdk.networks().getNetworkByType(networkTypeOrId = networkType, isElectrum = isElectrum).id
        return copy(network = gdk.networks().getNetworkById(id), networkType = networkType, isSinglesig = isElectrum)
    }

    override fun toString(): String {
        return "SetupArgs(mnemonic=**Redacted**, password=**Redacted**, isRestoreFlow=$isRestoreFlow, isWatchOnly=$isWatchOnly, isTestnet=$isTestnet, isSinglesig=$isSinglesig, networkType=$networkType, greenWallet=$greenWallet, assetId=$assetId, network=$network, accountType=$accountType, xpub=**Redacted**, page=$page, isShowRecovery=$isShowRecovery, isLightning=$isLightning)"
    }

    companion object {
        fun restoreMnemonic(greenWallet: GreenWallet): SetupArgs{
            return SetupArgs(
                isRestoreFlow = true,
                isWatchOnly = false,
                isTestnet = greenWallet.isTestnet,
                greenWallet = greenWallet
            )
        }
    }
}