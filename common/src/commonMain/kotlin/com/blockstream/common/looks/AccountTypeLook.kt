package com.blockstream.common.looks

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_2fa_protected
import blockstream_green.common.generated.resources.id_2of2
import blockstream_green.common.generated.resources.id_2of3
import blockstream_green.common.generated.resources.id_2of3_with_2fa
import blockstream_green.common.generated.resources.id_account_for_special_assets
import blockstream_green.common.generated.resources.id_amp
import blockstream_green.common.generated.resources.id_cheaper_singlesig_option
import blockstream_green.common.generated.resources.id_fast_transactions_on_the
import blockstream_green.common.generated.resources.id_fastest
import blockstream_green.common.generated.resources.id_legacy
import blockstream_green.common.generated.resources.id_legacy_account
import blockstream_green.common.generated.resources.id_legacy_segwit
import blockstream_green.common.generated.resources.id_lightning
import blockstream_green.common.generated.resources.id_multisig
import blockstream_green.common.generated.resources.id_native_segwit
import blockstream_green.common.generated.resources.id_permanent_2fa_account_ideal_for
import blockstream_green.common.generated.resources.id_quick_setup_2fa_account_ideal
import blockstream_green.common.generated.resources.id_simple_portable_standard
import blockstream_green.common.generated.resources.id_singlesig
import blockstream_green.common.generated.resources.id_standard
import blockstream_green.common.generated.resources.id_taproot
import blockstream_green.common.generated.resources.id_unknown
import blockstream_green.common.generated.resources.key_multisig
import blockstream_green.common.generated.resources.key_singlesig
import blockstream_green.common.generated.resources.lightning_fill
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.AccountType
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Serializable
data class AccountTypeLook(
    val accountType: AccountType,
    val canBeAdded: Boolean = true
) : GreenJson<AccountTypeLook>() {
    override fun kSerializer() = serializer()

    val isMultisig
        get() = accountType.isMutlisig()

    val isSinglesig
        get() = accountType.isSinglesig()

    val isLightning
        get() = accountType.isLightning()

    val title: StringResource
        get() = when (accountType) {
            AccountType.STANDARD -> Res.string.id_2fa_protected
            AccountType.AMP_ACCOUNT -> Res.string.id_amp
            AccountType.TWO_OF_THREE -> Res.string.id_2of3_with_2fa
            AccountType.BIP44_LEGACY -> Res.string.id_legacy
            AccountType.BIP49_SEGWIT_WRAPPED -> Res.string.id_legacy_segwit
            AccountType.BIP84_SEGWIT -> Res.string.id_standard
            AccountType.BIP86_TAPROOT -> Res.string.id_taproot
            AccountType.LIGHTNING -> Res.string.id_lightning
            else -> Res.string.id_unknown
        }

    val description: StringResource
        get() = when (accountType) {
            AccountType.STANDARD -> Res.string.id_quick_setup_2fa_account_ideal
            AccountType.AMP_ACCOUNT -> Res.string.id_account_for_special_assets
            AccountType.TWO_OF_THREE -> Res.string.id_permanent_2fa_account_ideal_for
            AccountType.BIP44_LEGACY -> Res.string.id_legacy_account
            AccountType.BIP49_SEGWIT_WRAPPED -> Res.string.id_simple_portable_standard
            AccountType.BIP84_SEGWIT -> Res.string.id_cheaper_singlesig_option
            AccountType.BIP86_TAPROOT -> Res.string.id_cheaper_singlesig_option
            AccountType.LIGHTNING -> Res.string.id_fast_transactions_on_the
            else -> Res.string.id_unknown
        }

    val policy: StringResource
        get() = when (accountType) {
            AccountType.STANDARD -> Res.string.id_2of2
            AccountType.AMP_ACCOUNT -> Res.string.id_amp
            AccountType.TWO_OF_THREE -> Res.string.id_2of3
            AccountType.BIP44_LEGACY -> Res.string.id_legacy
            AccountType.BIP49_SEGWIT_WRAPPED -> Res.string.id_legacy_segwit
            AccountType.BIP84_SEGWIT -> Res.string.id_native_segwit
            AccountType.BIP86_TAPROOT -> Res.string.id_taproot
            AccountType.LIGHTNING -> Res.string.id_fastest
            else -> Res.string.id_unknown
        }

    val security: StringResource
        get() = when {
            accountType.isMutlisig() -> Res.string.id_multisig
            accountType.isLightning() -> Res.string.id_lightning
            else -> Res.string.id_singlesig
        }

    fun icon(): DrawableResource = when {
        isMultisig -> Res.drawable.key_multisig
        isLightning -> Res.drawable.lightning_fill
        else -> Res.drawable.key_singlesig
    }
}