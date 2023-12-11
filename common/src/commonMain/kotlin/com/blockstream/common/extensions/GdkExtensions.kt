package com.blockstream.common.extensions

import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.Denomination
import com.blockstream.common.gdk.GA_ERROR
import com.blockstream.common.gdk.GA_NOT_AUTHORIZED
import com.blockstream.common.gdk.GA_RECONNECT
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.utils.getBitcoinOrLiquidUnit

fun <T : Any> GdkSession.ifConnected(block: () -> T?): T? {
    return if (this.isConnected) {
        block()
    } else {
        null
    }
}

fun ByteArray.reverseBytes(): ByteArray {
    for (i in 0 until size / 2) {
        val b = this[i]
        this[i] = this[size - i - 1]
        this[size - i - 1] = b
    }
    return this
}

fun Transaction.getConfirmationsMax(session: GdkSession): Int {
    if(isLoadingTransaction) return 0
    return getConfirmations(session.block(network).value.height).coerceAtMost((if (network.isLiquid) 3 else 7)).toInt()
}

fun Transaction.getConfirmations(session: GdkSession): Long {
    if(isLoadingTransaction) return -1
    return getConfirmations(session.block(network).value.height)
}

fun AccountType?.title(): String = when (this) {
    AccountType.STANDARD -> "2FA Protected"
    AccountType.AMP_ACCOUNT -> "AMP"
    AccountType.TWO_OF_THREE -> "2of3 with 2FA"
    AccountType.BIP44_LEGACY -> "Legacy"
    AccountType.BIP49_SEGWIT_WRAPPED -> "Legacy SegWit"
    AccountType.BIP84_SEGWIT -> "Standard"
    AccountType.BIP86_TAPROOT -> "Taproot"
    AccountType.LIGHTNING -> "Lightning"
    else -> "Unknown"
}

fun Account.needs2faActivation(session: GdkSession): Boolean {
    return try {
        isMultisig && !isAmp && (!session.isWatchOnly && !session.getTwoFactorConfig(network = network, useCache = true).anyEnabled)
    }catch (e: Exception){
        e.printStackTrace()
        false
    }
}

fun String?.isPolicyAsset(network: Network?): Boolean = (this == null || this == network?.policyAsset)
fun String?.isPolicyAsset(session: GdkSession): Boolean = (isPolicyAsset(session.bitcoin) || isPolicyAsset(session.liquid))

// If no Bitcoin network is available, fallback to Liquid
fun String?.networkForAsset(session: GdkSession): Network = (if(this == null || this == BTC_POLICY_ASSET) (session.activeBitcoin ?: session.activeLiquid) else session.activeLiquid ) ?: session.defaultNetwork

fun String?.assetTicker(
    session: GdkSession,
    denomination: Denomination? = null
) = assetTickerOrNull(session = session, denomination = denomination) ?: ""

fun String?.assetTickerOrNull(
    session: GdkSession,
    denomination: Denomination? = null
): String? {
    return if (this.isPolicyAsset(session)) {
        getBitcoinOrLiquidUnit(session = session, assetId = this, denomination = denomination)
    } else {
        this?.let { session.getAsset(it)?.ticker }
    }
}

fun String?.assetIsAmp(session: GdkSession) = session.enrichedAssets.value[this]?.isAmp ?: false
fun String?.assetWeight(session: GdkSession) = session.enrichedAssets.value[this]?.weight ?: 0


fun Account.hasHistory(session: GdkSession): Boolean {
    return bip44Discovered == true || isFunded(session) || session.accountTransactions(this).let {
        it.value.isNotEmpty() && it.value.firstOrNull()?.isLoadingTransaction == false
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

fun Throwable.isNotAuthorized() =
    getGDKErrorCode() == GA_NOT_AUTHORIZED || message == "id_invalid_pin"

fun Throwable.isConnectionError() = message?.contains("failed to connect") ?: false