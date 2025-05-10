package com.blockstream.common.extensions

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_authenticator_app
import blockstream_green.common.generated.resources.id_call
import blockstream_green.common.generated.resources.id_email
import blockstream_green.common.generated.resources.id_sms
import blockstream_green.common.generated.resources.id_telegram
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.LN_BTC_POLICY_ASSET
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.Database
import com.blockstream.common.gdk.GA_ERROR
import com.blockstream.common.gdk.GA_NOT_AUTHORIZED
import com.blockstream.common.gdk.GA_RECONNECT
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.utils.getBitcoinOrLiquidUnit
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import org.jetbrains.compose.resources.StringResource

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
    if (isSinglesig || isAmp || session.isWatchOnlyValue) {
        return false
    }

    return network.needs2faActivation(session = session)
}

fun Network.needs2faActivation(session: GdkSession): Boolean {
    return try {
        !session.isWatchOnlyValue && session.getTwoFactorConfig(network = this)?.anyEnabled == false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Account.hasExpiredUtxos(session: GdkSession): Boolean {
    return !session.isWatchOnlyValue && isMultisig && session.expired2FA.value.contains(this)
}

fun Account.hasTwoFactorReset(session: GdkSession): Boolean {
    return isMultisig && session.twoFactorReset(network).value?.isActive == true
}

fun String?.isBitcoinPolicyAsset(): Boolean = (this == null || this == BTC_POLICY_ASSET)
fun String?.isLightningPolicyAsset(): Boolean = (this == LN_BTC_POLICY_ASSET)
fun String?.isPolicyAsset(network: Network?): Boolean = (this == null || this == network?.policyAsset)
fun String?.isPolicyAsset(session: GdkSession): Boolean = isBitcoinPolicyAsset() || isLightningPolicyAsset() || session.gdkSessions.keys.any { isPolicyAsset(it) }

// If no Bitcoin network is available, fallback to Liquid
fun String?.networkForAsset(session: GdkSession): Network? = when {
    isBitcoinPolicyAsset() -> session.activeBitcoin ?: session.bitcoin
    else -> {
        session.activeLiquid ?: session.liquid
    }
}

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
    return bip44Discovered == true || isFunded(session) || session.accountTransactions(this).value.isNotEmpty()
}

fun String.getAssetNameOrNull(session: GdkSession?): String? {
    return if(session == null || this.isPolicyAsset(session)) {
        if(this == BTC_POLICY_ASSET){
            "Bitcoin"
        }else{
            "Liquid Bitcoin"
        }.let {
            if(session?.isTestnet == true) "Testnet $it" else it
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
            "LBTC"
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

fun String.twoFactorMethodsLocalizedDeprecated(): String = when (this) {
    "phone" -> "id_call"
    "gauth" -> "id_authenticator_app"
    else -> {
        "id_$this"
    }
}

fun String.twoFactorMethodsLocalized(): StringResource = when (this) {
    "email" -> Res.string.id_email
    "phone" -> Res.string.id_call
    "telegram" -> Res.string.id_telegram
    "gauth" -> Res.string.id_authenticator_app
    else -> Res.string.id_sms
}

fun List<String>.twoFactorMethodsLocalizedDeprecated(): List<String> = map {
    it.twoFactorMethodsLocalizedDeprecated()
}

fun List<String>.twoFactorMethodsLocalized(): List<StringResource> = map {
    it.twoFactorMethodsLocalized()
}

suspend fun GdkSession.getWallet(database: Database, sessionManager: SessionManager): GreenWallet? {
    return (ephemeralWallet ?: (sessionManager.getWalletIdFromSession(this)?.let { walletId -> database.getWallet(walletId) }))
}