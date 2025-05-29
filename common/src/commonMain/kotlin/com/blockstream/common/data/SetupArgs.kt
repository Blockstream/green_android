package com.blockstream.common.data

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Credentials
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.navigation.PopTo
import kotlinx.serialization.Serializable

@Serializable
data class SetupArgs constructor(
    val mnemonic: String = "",
    val password: String? = null,
    val isRestoreFlow: Boolean = false,
    val isWatchOnly: Boolean = false,
    val isTestnet: Boolean? = null,
    val isSinglesig: Boolean? = null,
    val isRecoveryConfirmed: Boolean = true,
    val greenWallet: GreenWallet? = null,
    val assetId: String? = null,
    val network: Network? = null,
    val accountType: AccountType? = null,
    val credentials: Credentials? = null,
    val xpub: String? = null,
    val page: Int = 1,
    val isShowRecovery: Boolean = false,
    val isLightning: Boolean = false,
    val popTo: PopTo? = null
) : GreenJson<SetupArgs>(), Redact {

    override fun kSerializer() = serializer()

    val mnemonicAsWords
        get() = mnemonic.split(" ")

    val isGenerateMnemonic
        get() = mnemonic.isEmpty() && !isShowRecovery

    val isRecoveryFlow
        get() = isRestoreFlow && greenWallet != null

    fun nextPage() = copy(page = page + 1)

    fun pageOne() = copy(page = 1)

    fun isAddAccount() = greenWallet != null && network != null && assetId != null

    companion object {
        fun restoreMnemonic(greenWallet: GreenWallet): SetupArgs {
            return SetupArgs(
                isRestoreFlow = true,
                isWatchOnly = false,
                isTestnet = greenWallet.isTestnet,
                greenWallet = greenWallet
            )
        }
    }
}