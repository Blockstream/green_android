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
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore

inline fun <T : Any> GdkSession.ifConnected(block: () -> T?): T? {
    return if (this.isConnected) {
        block()
    } else {
        null
    }
}

@NativeCoroutinesIgnore
suspend fun <T : Any> GdkSession.ifConnectedSuspend(block: suspend () -> T?): T? {
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
    return getConfirmations(session.block(network).value.height).coerceAtMost((if (network.isLiquid) 3 else 7)).toInt()
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

fun AccountType.policyRes(): String = when (this) {
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

fun AccountType.policyAndType(): String = when {
    this.isMutlisig() -> "id_multisig__|${policyRes()}"
    this.isLightning() -> "id_lightning"
    else -> "id_singlesig__|${policyRes()}"
}

fun Account.needs2faActivation(session: GdkSession): Boolean {
    if (isSinglesig || isAmp || session.isWatchOnly) {
        return false
    }

    return network.needs2faActivation(session = session)
}

fun Network.needs2faActivation(session: GdkSession): Boolean {
        return try {
        session.getTwoFactorConfig(network = this)?.anyEnabled == false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Account.hasExpiredUtxos(session: GdkSession): Boolean {
    return !session.isWatchOnly && isMultisig && session.expired2FA.value.contains(this)
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

fun Account.hasHistory(session: GdkSession): Boolean {
    return bip44Discovered == true || isFunded(session) || session.accountTransactions(this).let {
        it.value.isNotEmpty()
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

@Deprecated("Use EnrichedAsset")
fun String.getAssetName(session: GdkSession): String {
    return getAssetNameOrNull(session) ?: this
}

@Deprecated("Use EnrichedAsset")
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

fun String.twoFactorMethodsLocalized(): String = when (this) {
    "phone" -> "id_call"
    "gauth" -> "id_authenticator_app"
    else -> {
        "id_$this"
    }
}

fun List<String>.twoFactorMethodsLocalized(): List<String> = map {
    it.twoFactorMethodsLocalized()
}
