package com.blockstream.green.gdk

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.WalletIcon
import com.blockstream.common.database.Database
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GA_ERROR
import com.blockstream.common.gdk.GA_NOT_AUTHORIZED
import com.blockstream.common.gdk.GA_RECONNECT
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Device
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.managers.SessionManager
import com.blockstream.green.R
import com.blockstream.green.extensions.toBitmap
import com.blockstream.green.extensions.toBitmapDrawable

// Should we cache BitmapDrawable/Bitmap
fun GdkSession.getAssetDrawableOrNull(context: Context, assetId: String): Drawable? {
    networkAssetManager.getAssetIcon(assetId, this)?.let {
        return it.toBitmap()?.toBitmapDrawable(context)
    }

    return null
}

suspend fun GdkSession.getWallet(database: Database, sessionManager: SessionManager): GreenWallet? {
    return (ephemeralWallet ?: (sessionManager.getWalletIdFromSession(this)?.let { walletId -> database.getWallet(walletId) }))
}

fun GdkSession.getAssetDrawableOrDefault(context: Context, assetId: String): Drawable {
    return getAssetDrawableOrNull(context, assetId) ?: context.getDrawable(R.drawable.ic_unknown)!!
}

fun AccountType?.titleRes(): Int = when (this) {
    AccountType.STANDARD -> R.string.id_2fa_protected
    AccountType.AMP_ACCOUNT -> R.string.id_amp
    AccountType.TWO_OF_THREE -> R.string.id_2of3_with_2fa
    AccountType.BIP44_LEGACY -> R.string.id_legacy
    AccountType.BIP49_SEGWIT_WRAPPED -> R.string.id_legacy_segwit
    AccountType.BIP84_SEGWIT -> R.string.id_standard
    AccountType.BIP86_TAPROOT -> R.string.id_taproot
    AccountType.LIGHTNING -> R.string.id_lightning
    else -> R.string.id_unknown
}

fun AccountType?.policyRes(): Int = when (this) {
    AccountType.STANDARD -> R.string.id_2of2
    AccountType.AMP_ACCOUNT -> R.string.id_amp
    AccountType.TWO_OF_THREE -> R.string.id_2of3
    AccountType.BIP44_LEGACY -> R.string.id_legacy
    AccountType.BIP49_SEGWIT_WRAPPED -> R.string.id_legacy_segwit
    AccountType.BIP84_SEGWIT -> R.string.id_native_segwit
    AccountType.BIP86_TAPROOT -> R.string.id_taproot
    AccountType.LIGHTNING -> R.string.id_fastest
    else -> R.string.id_unknown
}

fun AccountType.policyType(context: Context): String = context.getString(policyRes())

fun AccountType.securityType(context: Context): String = when {
    this.isMutlisig() -> "${context.getString(R.string.id_multisig)}"
    this.isLightning() -> "${context.getString(R.string.id_lightning)}"
    else -> "${context.getString(R.string.id_singlesig)}"
}

fun Account.typeWithPolicyAndNumber(context: Context): String = type.withPolicy(context).let { type ->
    when{
        isMultisig || isLightning -> type
        else -> "$type #$accountNumber"
    }
}

fun AccountType?.descriptionRes(): Int = when (this) {
    AccountType.STANDARD -> R.string.id_quick_setup_2fa_account_ideal
    AccountType.AMP_ACCOUNT -> R.string.id_account_for_special_assets
    AccountType.TWO_OF_THREE -> R.string.id_permanent_2fa_account_ideal_for
    AccountType.BIP44_LEGACY -> R.string.id_legacy_account
    AccountType.BIP49_SEGWIT_WRAPPED -> R.string.id_simple_portable_standard
    AccountType.BIP84_SEGWIT -> R.string.id_cheaper_singlesig_option
    AccountType.BIP86_TAPROOT -> R.string.id_cheaper_singlesig_option
    AccountType.LIGHTNING -> R.string.id_fast_transactions_on_the
    else -> R.string.id_unknown
}

fun AccountType.withPolicy(context: Context): String = policyRes().let {
    when{
        this.isMutlisig() -> "${context.getString(R.string.id_multisig)} / ${context.getString(it)}"
        this.isLightning() -> "${context.getString(R.string.id_lightning)}"
        else -> "${context.getString(R.string.id_singlesig)} / ${context.getString(it)}"
    }
}

fun Network.getNetworkIcon(): Int{
    return id.getNetworkIcon()
}

fun Long?.getDirectionColor(context: Context): Int = ContextCompat.getColor(context, if ((this ?: 0) < 0) R.color.white else R.color.brand_green)


fun String.getNetworkIcon(): Int{
    if (Network.isBitcoinMainnet(this)) return R.drawable.ic_bitcoin
    if (Network.isLiquidMainnet(this)) return R.drawable.ic_liquid
    if (Network.isBitcoinTestnet(this)) return R.drawable.ic_bitcoin_testnet
    if (Network.isLiquidTestnet(this)) return R.drawable.ic_liquid_testnet
    if (Network.isLightningMainnet(this)) return R.drawable.ic_bitcoin_lightning
    if (Network.isLightningTestnet(this)) return R.drawable.ic_bitcoin_lightning_testnet
    return R.drawable.ic_unknown
}

fun String.getNetworkColor(): Int = when {
    Network.isBitcoinMainnet(this) -> R.color.bitcoin
    Network.isLiquidMainnet(this) -> R.color.liquid
    Network.isLightning(this) -> R.color.lightning
    Network.isLightningTestnet(this) -> R.color.lightning_testnet
    Network.isLiquidTestnet(this) -> R.color.liquid_testnet
    Network.isBitcoinTestnet(this) -> R.color.bitcoin_testnet
    else -> R.color.bitcoin_testnet
}

fun Account.getAccountColor(context: Context): Int = when {
    isAmp && isLiquidMainnet -> R.color.amp
    isAmp && isLiquidTestnet -> R.color.amp_testnet
    else -> networkId.getNetworkColor()
}.let {
    ContextCompat.getColor(context, it)
}

fun String?.getAssetIcon(context: Context, session: GdkSession, isLightning: Boolean = false): Drawable {
    return if (this == null || this.isPolicyAsset(session)) {
        ContextCompat.getDrawable(
            context,
            if (this == null || this == BTC_POLICY_ASSET) {
                if(isLightning){
                    if (session.isMainnet) {
                        R.drawable.ic_bitcoin_lightning
                    } else {
                        R.drawable.ic_bitcoin_lightning_testnet
                    }
                }else{
                    if (session.isMainnet) {
                        R.drawable.ic_bitcoin
                    } else {
                        R.drawable.ic_bitcoin_testnet
                    }
                }
            } else {
                if (session.isMainnet) {
                    R.drawable.ic_liquid
                } else {
                    R.drawable.ic_liquid_testnet
                }
            }
        )!!
    } else {
        session.liquid?.let { session.getAssetDrawableOrDefault(context,this) }
            ?: ContextCompat.getDrawable(context, R.drawable.ic_unknown)!!
    }
}

fun Device.getIcon(): Int{
    return when {
        isTrezor -> R.drawable.trezor_device
        isLedger -> R.drawable.ledger_device
        else -> R.drawable.blockstream_jade_device
    }
}

fun com.blockstream.green.devices.Device.getIcon(): Int{
    return when {
        deviceBrand.isTrezor -> R.drawable.trezor_device
        deviceBrand.isLedger -> R.drawable.ledger_device
        else -> R.drawable.blockstream_jade_device
    }
}

fun GreenWallet.iconResource() = when(icon) {
    WalletIcon.WATCH_ONLY -> R.drawable.ic_regular_eye_24
    WalletIcon.TESTNET -> R.drawable.ic_regular_flask_24
    WalletIcon.BIP39 -> R.drawable.ic_regular_wallet_passphrase_24
    WalletIcon.HARDWARE -> R.drawable.ic_regular_hww_24 // session.device!!.getIcon()
    WalletIcon.LIGHTNING -> R.drawable.ic_lightning_fill
    else -> R.drawable.ic_regular_wallet_24
}

fun Throwable.getGDKErrorCode(): Int {
    return this.message?.getGDKErrorCode() ?: GA_ERROR
}
fun String.getGDKErrorCode(): Int {
    return try {
        val stringCode = this.split(" ".toRegex()).toTypedArray()[1]
        val function = this.split(" ".toRegex()).toTypedArray()[2]
        val code = stringCode.toInt()
        // remap gdk connection error
        if (code == GA_ERROR && "GA_connect" == function) GA_RECONNECT else code
    } catch (e: Exception) {
        GA_ERROR
    }
}

fun Throwable.isNotAuthorized() = (message ?: "").isNotAuthorized()
fun String.isNotAuthorized() =
    getGDKErrorCode() == GA_NOT_AUTHORIZED || this == "id_invalid_pin"

fun String.isConnectionError() = this.contains("failed to connect") || this == "id_connection_failed"
fun Throwable.isConnectionError() = (message ?: "").isConnectionError()


