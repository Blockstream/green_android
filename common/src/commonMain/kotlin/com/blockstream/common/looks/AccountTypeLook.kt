package com.blockstream.common.looks

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.AccountType
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class AccountTypeLook(
    val accountType: AccountType,
    val canBeAdded: Boolean = true
) : GreenJson<AccountTypeLook>(), Parcelable {
    override fun kSerializer() = serializer()

    val isMultisig
        get() = accountType.isMutlisig()

    val isSinglesig
        get() = accountType.isSinglesig()

    val isLightning
        get() = accountType.isLightning()

    val title: String
        get() = when (accountType) {
            AccountType.STANDARD -> "id_2fa_protected"
            AccountType.AMP_ACCOUNT -> "id_amp"
            AccountType.TWO_OF_THREE -> "id_2of3_with_2fa"
            AccountType.BIP44_LEGACY -> "id_legacy"
            AccountType.BIP49_SEGWIT_WRAPPED -> "id_legacy_segwit"
            AccountType.BIP84_SEGWIT -> "id_standard"
            AccountType.BIP86_TAPROOT -> "id_taproot"
            AccountType.LIGHTNING -> "id_lightning"
            else -> "id_unknown"
        }

    val description: String
        get() = when (accountType) {
            AccountType.STANDARD -> "id_quick_setup_2fa_account_ideal"
            AccountType.AMP_ACCOUNT -> "id_account_for_special_assets"
            AccountType.TWO_OF_THREE -> "id_permanent_2fa_account_ideal_for"
            AccountType.BIP44_LEGACY -> "id_legacy_account"
            AccountType.BIP49_SEGWIT_WRAPPED -> "id_simple_portable_standard"
            AccountType.BIP84_SEGWIT -> "id_cheaper_singlesig_option"
            AccountType.BIP86_TAPROOT -> "id_cheaper_singlesig_option"
            AccountType.LIGHTNING -> "id_fast_transactions_on_the"
            else -> "id_unknown"
        }

    val security: String
        get() = when {
            accountType.isMutlisig() -> "id_multisig"
            accountType.isLightning() -> "id_lightning"
            else -> "id_singlesig"
        }

    val policy: String
        get() = when (accountType) {
            AccountType.STANDARD -> "id_2of2"
            AccountType.AMP_ACCOUNT -> "id_amp"
            AccountType.TWO_OF_THREE -> "id_2of3"
            AccountType.BIP44_LEGACY -> "id_legacy"
            AccountType.BIP49_SEGWIT_WRAPPED -> "id_legacy_segwit"
            AccountType.BIP84_SEGWIT -> "id_native_segwit"
            AccountType.BIP86_TAPROOT -> "id_taproot"
            AccountType.LIGHTNING -> "id_fastest"
            else -> "id_unknown"
        }

}