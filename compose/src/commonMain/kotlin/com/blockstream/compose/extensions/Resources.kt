package com.blockstream.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.at
import blockstream_green.common.generated.resources.bitcoin
import blockstream_green.common.generated.resources.bitcoin_lightning
import blockstream_green.common.generated.resources.bitcoin_lightning_testnet
import blockstream_green.common.generated.resources.bitcoin_testnet
import blockstream_green.common.generated.resources.blockstream_devices
import blockstream_green.common.generated.resources.blockstream_jade_action
import blockstream_green.common.generated.resources.blockstream_jade_device
import blockstream_green.common.generated.resources.blockstream_jade_plus_action
import blockstream_green.common.generated.resources.blockstream_jade_plus_device
import blockstream_green.common.generated.resources.code_block
import blockstream_green.common.generated.resources.currency_btc
import blockstream_green.common.generated.resources.eye
import blockstream_green.common.generated.resources.eye_slash
import blockstream_green.common.generated.resources.flask
import blockstream_green.common.generated.resources.generic_device
import blockstream_green.common.generated.resources.id_2of2
import blockstream_green.common.generated.resources.id_2of3
import blockstream_green.common.generated.resources.id_amp
import blockstream_green.common.generated.resources.id_fastest
import blockstream_green.common.generated.resources.id_invalid_spv
import blockstream_green.common.generated.resources.id_legacy
import blockstream_green.common.generated.resources.id_legacy_segwit
import blockstream_green.common.generated.resources.id_lightning
import blockstream_green.common.generated.resources.id_multisig__s
import blockstream_green.common.generated.resources.id_native_segwit
import blockstream_green.common.generated.resources.id_not_on_longest_chain
import blockstream_green.common.generated.resources.id_singlesig__s
import blockstream_green.common.generated.resources.id_taproot
import blockstream_green.common.generated.resources.id_transaction_completed
import blockstream_green.common.generated.resources.id_transaction_confirmed_ss
import blockstream_green.common.generated.resources.id_transaction_failed
import blockstream_green.common.generated.resources.id_unknown
import blockstream_green.common.generated.resources.id_verified
import blockstream_green.common.generated.resources.id_verifying
import blockstream_green.common.generated.resources.key_multisig
import blockstream_green.common.generated.resources.key_singlesig
import blockstream_green.common.generated.resources.ledger_device
import blockstream_green.common.generated.resources.lightning_fill
import blockstream_green.common.generated.resources.liquid
import blockstream_green.common.generated.resources.liquid_testnet
import blockstream_green.common.generated.resources.qr_code
import blockstream_green.common.generated.resources.spv_error
import blockstream_green.common.generated.resources.spv_in_progress
import blockstream_green.common.generated.resources.spv_verified
import blockstream_green.common.generated.resources.spv_warning
import blockstream_green.common.generated.resources.text_aa
import blockstream_green.common.generated.resources.trezor_device
import blockstream_green.common.generated.resources.two_factor_authenticator
import blockstream_green.common.generated.resources.two_factor_call
import blockstream_green.common.generated.resources.two_factor_email
import blockstream_green.common.generated.resources.two_factor_sms
import blockstream_green.common.generated.resources.unknown
import blockstream_green.common.generated.resources.wallet
import blockstream_green.common.generated.resources.wallet_hw
import blockstream_green.common.generated.resources.wallet_passphrase
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.LBTC_POLICY_ASSET
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.WalletIcon
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.looks.transaction.Completed
import com.blockstream.common.looks.transaction.Confirmed
import com.blockstream.common.looks.transaction.Failed
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.looks.transaction.TransactionStatus
import com.blockstream.common.looks.transaction.Unconfirmed
import com.blockstream.compose.theme.amp
import com.blockstream.compose.theme.amp_testnet
import com.blockstream.compose.theme.bitcoin
import com.blockstream.compose.theme.bitcoin_testnet
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.lightning
import com.blockstream.compose.theme.liquid
import com.blockstream.compose.theme.liquid_testnet
import com.blockstream.compose.theme.orange
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.textHigh
import com.blockstream.compose.utils.toPainter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun WalletIcon.resource() = when (this) {
    WalletIcon.WATCH_ONLY -> Res.drawable.eye
    WalletIcon.TESTNET -> Res.drawable.flask
    WalletIcon.BIP39 -> Res.drawable.wallet_passphrase
    WalletIcon.HARDWARE -> Res.drawable.wallet_hw
    WalletIcon.LIGHTNING -> Res.drawable.lightning_fill
    WalletIcon.QR -> Res.drawable.qr_code
    else -> Res.drawable.wallet
}

fun GreenDevice?.icon(): DrawableResource = this?.deviceModel?.icon() ?: this?.deviceBrand?.deviceBrandIcon() ?: Res.drawable.generic_device

fun GreenDevice?.actionIcon(): DrawableResource = this?.deviceModel?.actionIcon() ?: this?.deviceBrand?.deviceBrandIcon() ?: Res.drawable.generic_device

fun DeviceBrand.deviceBrandIcon(): DrawableResource = when (this) {
    DeviceBrand.Ledger -> Res.drawable.ledger_device
    DeviceBrand.Trezor -> Res.drawable.trezor_device
    DeviceBrand.Generic -> Res.drawable.generic_device
    else -> Res.drawable.blockstream_devices
}

fun DeviceModel.icon(): DrawableResource = when (this) {
    DeviceModel.BlockstreamGeneric -> Res.drawable.blockstream_devices
    DeviceModel.BlockstreamJade -> Res.drawable.blockstream_jade_device
    DeviceModel.BlockstreamJadePlus -> Res.drawable.blockstream_jade_plus_device
    DeviceModel.TrezorGeneric, DeviceModel.TrezorModelT, DeviceModel.TrezorModelOne ->  Res.drawable.trezor_device
    DeviceModel.LedgerGeneric, DeviceModel.LedgerNanoS, DeviceModel.LedgerNanoX -> Res.drawable.ledger_device
    DeviceModel.Generic -> Res.drawable.generic_device
}

fun DeviceModel.actionIcon(): DrawableResource = when (this) {
    DeviceModel.BlockstreamGeneric -> Res.drawable.blockstream_jade_plus_action
    DeviceModel.BlockstreamJade -> Res.drawable.blockstream_jade_action
    DeviceModel.BlockstreamJadePlus -> Res.drawable.blockstream_jade_plus_action
    else -> icon()
}

fun String.getNetworkIcon(): DrawableResource {
    if (Network.isBitcoinMainnet(this)) return Res.drawable.bitcoin
    if (Network.isLiquidMainnet(this)) return Res.drawable.liquid
    if (Network.isBitcoinTestnet(this)) return Res.drawable.bitcoin_testnet
    if (Network.isLiquidTestnet(this)) return Res.drawable.liquid_testnet
    if (Network.isLightningMainnet(this)) return Res.drawable.bitcoin_lightning
    return Res.drawable.unknown
}

fun String.getNetworkColor(): Color = when {
    Network.isBitcoinMainnet(this) -> bitcoin
    Network.isLiquidMainnet(this) -> liquid
    Network.isLightning(this) -> lightning
    Network.isLiquidTestnet(this) -> liquid_testnet
    Network.isBitcoinTestnet(this) -> bitcoin_testnet
    else -> bitcoin_testnet
}

fun Account.getAccountColor(): Color = when {
    isAmp && isLiquidMainnet -> amp
    isAmp && isLiquidTestnet -> amp_testnet
    else -> networkId.getNetworkColor()
}

fun Account.policyIcon(): DrawableResource {
    return when {
        isLightning -> Res.drawable.lightning_fill
        isSinglesig -> Res.drawable.key_singlesig
        else -> Res.drawable.key_multisig
    }
}

fun TwoFactorMethod.getIcon(): DrawableResource = when (this) {
    TwoFactorMethod.EMAIL -> Res.drawable.two_factor_email
    TwoFactorMethod.SMS -> Res.drawable.two_factor_sms
    TwoFactorMethod.PHONE -> Res.drawable.two_factor_call
    TwoFactorMethod.AUTHENTICATOR -> Res.drawable.two_factor_authenticator
    TwoFactorMethod.TELEGRAM -> Res.drawable.two_factor_sms
}

fun Network.icon(): DrawableResource = network.getNetworkIcon()

fun TransactionStatus.color() = when (this) {
    is Failed -> red
    is Unconfirmed -> orange
    else -> green
}

@Composable
fun TransactionStatus.title() = when (this) {
    is Unconfirmed -> stringResource(Res.string.id_transaction_confirmed_ss, "0", confirmationsRequired.toString())
    is Confirmed -> stringResource(Res.string.id_transaction_confirmed_ss, confirmations.toString(), confirmationsRequired.toString())
    is Completed -> stringResource(Res.string.id_transaction_completed)
    is Failed -> stringResource(Res.string.id_transaction_failed)
}

fun Transaction.SPVResult.icon() = when (this) {
    Transaction.SPVResult.InProgress, Transaction.SPVResult.Unconfirmed -> Res.drawable.spv_in_progress
    Transaction.SPVResult.NotLongest -> Res.drawable.spv_warning
    Transaction.SPVResult.Verified -> Res.drawable.spv_verified
    else -> Res.drawable.spv_error
}

fun Transaction.SPVResult.title() = when (this) {
    Transaction.SPVResult.InProgress, Transaction.SPVResult.Unconfirmed -> Res.string.id_verifying
    Transaction.SPVResult.NotVerified -> Res.string.id_invalid_spv
    Transaction.SPVResult.NotLongest -> Res.string.id_not_on_longest_chain
    Transaction.SPVResult.Verified -> Res.string.id_verified
    else -> Res.string.id_invalid_spv
}

@Composable
fun TransactionLook.directionColor(index: Int) = when {
    transaction.isRefundableSwap -> red
    else -> if ((transaction.assets.getOrNull(index)?.second ?: 0) < 0) textHigh else green
}

@Composable
fun String?.assetIcon(session: GdkSession? = null, isLightning: Boolean = false): Painter {
    return if (this == null || this == BTC_POLICY_ASSET || this == LBTC_POLICY_ASSET || (session != null && this.isPolicyAsset(
            session
        ))
    ) {
        (if (this == null || this == BTC_POLICY_ASSET) {
            if (isLightning) {
                if (session?.isTestnet == true) {
                    Res.drawable.bitcoin_lightning_testnet
                } else {
                    Res.drawable.bitcoin_lightning
                }
            } else {
                if (session?.isTestnet == true) {
                    Res.drawable.bitcoin_testnet
                } else {
                    Res.drawable.bitcoin
                }
            }
        } else {
            if (session?.isTestnet == true) {
                Res.drawable.liquid_testnet
            } else {
                Res.drawable.liquid
            }
        }).let {
            painterResource(it)
        }
    } else {
        session?.networkAssetManager?.getAssetIcon(this, session)?.toPainter() ?: painterResource(Res.drawable.unknown)
    }
}

// Temp solution
fun String.toDrawableResource(): DrawableResource? = when(this) {
    "eye" -> Res.drawable.eye
    "eye_slash" -> Res.drawable.eye_slash
    "code_block" -> Res.drawable.code_block
    "currency_btc" -> Res.drawable.currency_btc
    "flask" -> Res.drawable.flask
    "text_aa" -> Res.drawable.text_aa
    "qr_code" -> Res.drawable.qr_code
    "at" -> Res.drawable.at
    else -> null
}

@Composable
fun AccountType.policyRes(): String = when (this) {
    AccountType.STANDARD -> stringResource(Res.string.id_2of2)
    AccountType.AMP_ACCOUNT -> stringResource(Res.string.id_amp)
    AccountType.TWO_OF_THREE -> stringResource(Res.string.id_2of3)
    AccountType.BIP44_LEGACY -> stringResource(Res.string.id_legacy)
    AccountType.BIP49_SEGWIT_WRAPPED -> stringResource(Res.string.id_legacy_segwit)
    AccountType.BIP84_SEGWIT -> stringResource(Res.string.id_native_segwit)
    AccountType.BIP86_TAPROOT -> stringResource(Res.string.id_taproot)
    AccountType.LIGHTNING -> stringResource(Res.string.id_fastest)
    else -> stringResource(Res.string.id_unknown)
}

@Composable
fun AccountType.policyAndType(): String = when {
    this.isMutlisig() -> stringResource(Res.string.id_multisig__s, policyRes())
    this.isLightning() -> stringResource(Res.string.id_lightning)
    else -> stringResource(Res.string.id_singlesig__s, policyRes())
}