package com.blockstream.common.data

import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Credentials
import com.blockstream.common.gdk.data.Network
import kotlinx.serialization.Serializable


@Parcelize
data class SetupArgs constructor(
    val mnemonic: String = "",
    val password: String? = null,
    val isRestoreFlow: Boolean = false,
    val isWatchOnly: Boolean = false,
    val isTestnet: Boolean? = null,
    val isSinglesig: Boolean? = null,
    val greenWallet: GreenWallet? = null,
    val assetId: String? = null,
    val network: Network? = null,
    val accountType: AccountType? = null,
    val credentials: Credentials? = null,
    val xpub: String? = null,
    val page: Int = 1,
    val isShowRecovery: Boolean = false,
    val isLightning: Boolean = false
) : Parcelable, JavaSerializable {

    val mnemonicAsWords
        get() = mnemonic.split(" ")

    val isGenerateMnemonic
        get() = mnemonic.isEmpty() && !isShowRecovery

    fun nextPage() = copy(page = page + 1)

    fun pageOne() = copy(page = 1)

    fun isAddAccount() = greenWallet != null && network != null && assetId != null

    override fun toString(): String {
        return "SetupArgs(mnemonic=**Redacted**, password=**Redacted**, isRestoreFlow=$isRestoreFlow, isWatchOnly=$isWatchOnly, isTestnet=$isTestnet, isSinglesig=$isSinglesig, greenWallet=$greenWallet, assetId=$assetId, network=$network, accountType=$accountType, xpub=**Redacted**, page=$page, isShowRecovery=$isShowRecovery, isLightning=$isLightning)"
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