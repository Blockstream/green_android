package com.blockstream.green.gdk

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.blockstream.gdk.BTC_POLICY_ASSET
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.AccountType
import com.blockstream.gdk.data.Device
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.utils.getBitcoinOrLiquidUnit
import com.blockstream.libgreenaddress.KotlinGDK

fun Transaction.getConfirmationsMax(session: GdkSession): Int {
    if(isLoadingTransaction) return 0
    return getConfirmations(session.blockHeight(network)).coerceAtMost((if (network.isLiquid) 3 else 7)).toInt()
}

fun Transaction.getConfirmations(session: GdkSession): Long {
    if(isLoadingTransaction) return -1
    return getConfirmations(session.blockHeight(network))
}

// By default policy asset is first
fun Assets.policyAsset() = entries.firstOrNull()?.value ?: 0

fun Assets.policyAssetOrNull() = entries.firstOrNull()?.value

fun AccountType?.titleRes(): Int = when (this) {
    AccountType.STANDARD -> R.string.id_2fa_protected
    AccountType.AMP_ACCOUNT -> R.string.id_amp
    AccountType.TWO_OF_THREE -> R.string.id_2of3_with_2fa
    AccountType.BIP44_LEGACY -> R.string.id_legacy
    AccountType.BIP49_SEGWIT_WRAPPED -> R.string.id_standard
    AccountType.BIP84_SEGWIT -> R.string.id_native_segwit
    AccountType.BIP86_TAPROOT -> R.string.id_taproot
    AccountType.LIGHTNING -> R.string.id_instant
    else -> R.string.id_unknown
}
fun AccountType?.title(): String = when (this) {
    AccountType.STANDARD -> "2FA Protected"
    AccountType.AMP_ACCOUNT -> "AMP"
    AccountType.TWO_OF_THREE -> "2of3 with 2FA"
    AccountType.BIP44_LEGACY -> "Legacy"
    AccountType.BIP49_SEGWIT_WRAPPED -> "Standard"
    AccountType.BIP84_SEGWIT -> "Native SegWit"
    AccountType.BIP86_TAPROOT -> "Taproot"
    AccountType.LIGHTNING -> "Instant"
    else -> "Unknown"
}

fun AccountType?.policyRes(): Int = when (this) {
    AccountType.STANDARD -> R.string.id_2of2
    AccountType.AMP_ACCOUNT -> R.string.id_amp
    AccountType.TWO_OF_THREE -> R.string.id_2of3
    AccountType.BIP44_LEGACY -> R.string.id_legacy
    AccountType.BIP49_SEGWIT_WRAPPED -> R.string.id_legacy_segwit
    AccountType.BIP84_SEGWIT -> R.string.id_native_segwit
    AccountType.BIP86_TAPROOT -> R.string.id_taproot
    AccountType.LIGHTNING -> R.string.id_instant
    else -> R.string.id_unknown
}

fun AccountType.withPolicy(context: Context): String = policyRes().let {
    if (this.isMutlisig()) {
        "${context.getString(R.string.id_multisig)} / ${context.getString(it)}"
    } else {
        "${context.getString(R.string.id_singlesig)} / ${context.getString(it)}"
    }
}

fun Account.typeWithPolicyAndNumber(context: Context): String = type.withPolicy(context).let { type ->
    if (isMultisig) {
        type
    } else {
        "$type #$accountNumber"
    }
}

fun Account.needs2faActivation(session: GdkSession): Boolean {
    return try {
        isMultisig && !isAmp && (!session.isWatchOnly && !session.getTwoFactorConfig(network = network, useCache = true).anyEnabled)
    }catch (e: Exception){
        e.printStackTrace()
        false
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
    else -> R.string.id_unknown
}

fun Network.getNetworkIcon(): Int{
    return id.getNetworkIcon()
}

fun Long?.getDirectionColor(context: Context): Int = ContextCompat.getColor(context, if ((this ?: 0) < 0) R.color.white else R.color.brand_green)

fun String?.isPolicyAsset(network: Network?): Boolean = (this == null || this == network?.policyAsset)
fun String?.isPolicyAsset(session: GdkSession): Boolean = (isPolicyAsset(session.bitcoin) || isPolicyAsset(session.liquid))

// If no Bitcoin network is available, fallback to Liquid
fun String?.networkForAsset(session: GdkSession): Network = (if(this == null || this == BTC_POLICY_ASSET) (session.activeBitcoin ?: session.activeLiquid) else session.activeLiquid ) ?: session.defaultNetwork

fun String?.assetTicker(session: GdkSession, overrideDenomination: String? = null) = assetTickerOrNull(session, overrideDenomination) ?: ""

fun String?.assetTickerOrNull(session: GdkSession, overrideDenomination: String? = null): String?{
    return if (this.isPolicyAsset(session)) {
        getBitcoinOrLiquidUnit(this, session, overrideDenomination = overrideDenomination)
    } else {
        this?.let { session.getAsset(it)?.ticker }
    }
}

fun String?.assetIsAmp(session: GdkSession) = session.enrichedAssets[this]?.isAmp ?: false
fun String?.assetWeight(session: GdkSession) = session.enrichedAssets[this]?.weight ?: 0

fun String.getNetworkIcon(): Int{
    if (Network.isBitcoinMainnet(this)) return R.drawable.ic_bitcoin
    if (Network.isLiquidMainnet(this)) return R.drawable.ic_liquid
    if (Network.isBitcoinTestnet(this)) return R.drawable.ic_bitcoin_testnet
    if (Network.isLiquidTestnet(this)) return R.drawable.ic_liquid_testnet
    return R.drawable.ic_unknown
}

fun String.getNetworkColor(): Int = when {
    Network.isBitcoinMainnet(this) -> R.color.bitcoin
    Network.isLiquidMainnet(this) -> R.color.liquid
    Network.isLiquidTestnet(this) -> R.color.liquid_testnet
    Network.isBitcoinTestnet(this) -> R.color.bitcoin_testnet
    else -> R.color.bitcoin_testnet
}

fun String.getNetworkColor(context: Context): Int = ContextCompat.getColor(context, getNetworkColor())

fun Account.getAccountColor(context: Context): Int = when {
    isAmp && isLiquidMainnet -> R.color.amp
    isAmp && isLiquidTestnet -> R.color.amp_testnet
    else -> networkId.getNetworkColor()
}.let {
    ContextCompat.getColor(context, it)
}

fun Account.isFunded(session: GdkSession): Boolean{
    return session.accountAssets(this).values.sum() > 0
}

fun Account.hasHistory(session: GdkSession): Boolean{
    return bip44Discovered == true || isFunded(session) || session.accountTransactions(this).let {
        it.isNotEmpty() && it.firstOrNull()?.isLoadingTransaction == false
    }
}

fun String?.getAssetIcon(context: Context, session: GdkSession): Drawable {

//    return if(this == null || this == BTC_POLICY_ASSET){
//        ContextCompat.getDrawable(
//            context,
//            if (session.isMainnet) {
//                R.drawable.ic_bitcoin
//            } else {
//                R.drawable.ic_bitcoin_testnet
//            }
//        )!!
//    }else{
//        session.liquid?.let { session.getAssetDrawableOrDefault(this) }
//            ?: ContextCompat.getDrawable(context, R.drawable.ic_unknown)!!
//    }

    return if (this == null || this.isPolicyAsset(session)) {
        ContextCompat.getDrawable(
            context,
            if (this == null || this == BTC_POLICY_ASSET) {
                if (session.isMainnet) {
                    R.drawable.ic_bitcoin
                } else {
                    R.drawable.ic_bitcoin_testnet
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
        session.liquid?.let { session.getAssetDrawableOrDefault(this) }
            ?: ContextCompat.getDrawable(context, R.drawable.ic_unknown)!!
    }
}

fun String.getAssetNameOrNull(session: GdkSession): String? {
    return if(this.isPolicyAsset(session)) {
        if(this == BTC_POLICY_ASSET){
            "Bitcoin"
        }else{
            "Liquid Bitcoin"
        }.let {
            if(session.isTestnet) "Testnet $it" else it
        }
    }else{
        session.liquid?.let { session.getAsset(this)?.name }
    }
}

fun String.getAssetName(session: GdkSession): String {
    return getAssetNameOrNull(session) ?: this
}

fun String.getAssetTicker(session: GdkSession): String? {
    return if(this.isPolicyAsset(session)) {
        if(this == BTC_POLICY_ASSET){
            "BTC"
        }else{
            "L-BTC"
        }.let {
            if(session.isTestnet) "TEST-$it" else it
        }
    }else{
        session.liquid?.let { session.getAsset(this)?.ticker }
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
        isTrezor -> R.drawable.trezor_device
        isLedger -> R.drawable.ledger_device
        else -> R.drawable.blockstream_jade_device
    }
}

fun Wallet.iconResource(session: GdkSession) = when {
    isWatchOnly -> R.drawable.ic_regular_eye_24
    isTestnet -> R.drawable.ic_regular_flask_24
    isBip39Ephemeral -> R.drawable.ic_regular_wallet_passphrase_24
    isHardware && session.device != null -> R.drawable.ic_regular_hww_24 // session.device!!.getIcon()
    session.gdkSessions.size == 1 -> if (session.mainAssetNetwork.isElectrum) R.drawable.ic_singlesig else R.drawable.ic_multisig
    else -> R.drawable.ic_regular_wallet_24
}

fun Throwable.getGDKErrorCode(): Int {
    return this.message?.getGDKErrorCode() ?: KotlinGDK.GA_ERROR
}
fun String.getGDKErrorCode(): Int {
    return try {
        val stringCode = this.split(" ".toRegex()).toTypedArray()[1]
        val function = this.split(" ".toRegex()).toTypedArray()[2]
        val code = stringCode.toInt()
        // remap gdk connection error
        if (code == KotlinGDK.GA_ERROR && "GA_connect" == function) KotlinGDK.GA_RECONNECT else code
    } catch (e: Exception) {
        KotlinGDK.GA_ERROR
    }
}

// TODO combine
fun Throwable.isNotAuthorized() =
    getGDKErrorCode() == KotlinGDK.GA_NOT_AUTHORIZED || message == "id_invalid_pin"
fun String.isNotAuthorized() =
    getGDKErrorCode() == KotlinGDK.GA_NOT_AUTHORIZED || this == "id_invalid_pin"

fun String.isConnectionError() = this.contains("failed to connect")