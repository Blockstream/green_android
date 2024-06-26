package com.blockstream.compose.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.LBTC_POLICY_ASSET
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.WalletIcon
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.device.DeviceInterface
import com.blockstream.common.looks.transaction.Completed
import com.blockstream.common.looks.transaction.Confirmed
import com.blockstream.common.looks.transaction.Failed
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.looks.transaction.TransactionStatus
import com.blockstream.common.looks.transaction.Unconfirmed
import com.blockstream.compose.R
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
import com.blockstream.compose.theme.whiteHigh

fun WalletIcon.resource() = when(this) {
    WalletIcon.WATCH_ONLY -> R.drawable.eye
    WalletIcon.TESTNET -> R.drawable.flask
    WalletIcon.BIP39 -> R.drawable.wallet_passphrase
    WalletIcon.HARDWARE -> R.drawable.wallet_hw
    WalletIcon.LIGHTNING -> R.drawable.lightning_fill
    else -> R.drawable.wallet
}

fun DeviceInterface.icon(): Int{
    return when {
        deviceBrand.isTrezor -> R.drawable.trezor_device
        deviceBrand.isLedger -> R.drawable.ledger_device
        else -> R.drawable.blockstream_jade_device
    }
}

fun String.getNetworkIcon(): Int{
    if (Network.isBitcoinMainnet(this)) return R.drawable.bitcoin
    if (Network.isLiquidMainnet(this)) return R.drawable.liquid
    if (Network.isBitcoinTestnet(this)) return R.drawable.bitcoin_testnet
    if (Network.isLiquidTestnet(this)) return R.drawable.liquid_testnet
    if (Network.isLightningMainnet(this)) return R.drawable.bitcoin_lightning
    return R.drawable.unknown
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

fun Account.policyIcon(): Int {
    return when{
        isLightning -> R.drawable.lightning_fill
        isSinglesig -> R.drawable.key_singlesig
        else -> R.drawable.key_multisig
    }
}

fun TwoFactorMethod.getIcon(): Int = when (this) {
    TwoFactorMethod.EMAIL -> R.drawable.two_factor_email
    TwoFactorMethod.SMS -> R.drawable.two_factor_sms
    TwoFactorMethod.PHONE -> R.drawable.two_factor_call
    TwoFactorMethod.AUTHENTICATOR -> R.drawable.two_factor_authenticator
    TwoFactorMethod.TELEGRAM -> R.drawable.two_factor_sms
}

fun Network.icon(): Int = network.getNetworkIcon()

fun TransactionStatus.color() = when(this) {
    is Failed -> red
    is Unconfirmed -> orange
    else -> green
}

fun TransactionStatus.title() = when(this) {
    is Unconfirmed -> "id_transaction_unconfirmed_s_s|0|${confirmationsRequired}"
    is Confirmed -> "id_transaction_confirmed_s_s|${confirmations}|${confirmationsRequired}"
    Completed -> "id_transaction_completed"
    is Failed -> "id_transaction_failed"
}

fun Transaction.SPVResult.icon() = when (this) {
    Transaction.SPVResult.InProgress, Transaction.SPVResult.Unconfirmed -> R.drawable.spv_in_progress
    Transaction.SPVResult.NotLongest -> R.drawable.spv_warning
    Transaction.SPVResult.Verified -> R.drawable.spv_verified
    else -> R.drawable.spv_error
}

fun Transaction.SPVResult.title() = when (this) {
    Transaction.SPVResult.InProgress, Transaction.SPVResult.Unconfirmed -> R.string.id_verifying_
    Transaction.SPVResult.NotVerified -> R.string.id_invalid_spv
    Transaction.SPVResult.NotLongest -> R.string.id_not_on_longest_chain
    Transaction.SPVResult.Verified -> R.string.id_verified
    else -> R.drawable.spv_error
}

fun TransactionLook.directionColor(index: Int) = when  {
    transaction.isRefundableSwap -> red
    else -> if((transaction.assets.getOrNull(index)?.second ?: 0) < 0) whiteHigh else green
}

private fun ByteArray?.toBitmap(): Bitmap? {
    if (this != null) {
        try {
            return BitmapFactory.decodeByteArray(this, 0, this.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}

private fun Bitmap.toPainter(): Painter {
    return BitmapPainter(asImageBitmap())
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
                    R.drawable.bitcoin_lightning_testnet
                } else {
                    R.drawable.bitcoin_lightning
                }
            } else {
                if (session?.isTestnet == true) {
                    R.drawable.bitcoin_testnet
                } else {
                    R.drawable.bitcoin
                }
            }
        } else {
            if (session?.isTestnet == true) {
                R.drawable.liquid_testnet
            } else {
                R.drawable.liquid
            }
        }).let {
            painterResource(id = it)
        }
    } else {
        session?.networkAssetManager?.getAssetIcon(this, session)?.let {
            it.toBitmap()?.toPainter()
        } ?: painterResource(id = R.drawable.unknown)
    }
}