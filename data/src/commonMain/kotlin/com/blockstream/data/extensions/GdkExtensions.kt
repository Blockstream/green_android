package com.blockstream.data.extensions

import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.LN_BTC_POLICY_ASSET
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GA_ERROR
import com.blockstream.data.gdk.GA_NOT_AUTHORIZED
import com.blockstream.data.gdk.GA_RECONNECT
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.utils.getBitcoinOrLiquidUnit

inline fun <T : Any> GdkSession.ifConnected(block: () -> T?): T? {
    return if (this.isConnected) {
        block()
    } else {
        null
    }
}

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

fun List<Account>.filterForAsset(assetId: String, session: GdkSession): List<Account> {
    val enrichedAsset = session.getEnrichedAssets(assetId)
    return filter { account ->
        when {
            enrichedAsset?.isAmp == true -> account.type == AccountType.AMP_ACCOUNT
            assetId.isPolicyAsset(session) -> account.network.policyAsset == assetId
            else -> account.isLiquid
        }
    }
}

fun String?.isBitcoinPolicyAsset(): Boolean = (this == null || this == BTC_POLICY_ASSET)
fun String?.isLightningPolicyAsset(): Boolean = (this == LN_BTC_POLICY_ASSET)
fun String?.isPolicyAsset(network: Network?): Boolean = (this == null || this == network?.policyAsset)
fun String?.isPolicyAsset(session: GdkSession): Boolean =
    isBitcoinPolicyAsset() || isLightningPolicyAsset() || session.gdkSessions.keys.any { isPolicyAsset(it) }

// If no Bitcoin network is available, fallback to Liquid
fun String?.networkForAsset(session: GdkSession): Network? = when {
    isBitcoinPolicyAsset() -> session.activeBitcoin ?: session.bitcoin
    isLightningPolicyAsset() -> session.lightning
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

fun Account.hasUnconfirmedTransactions(session: GdkSession): Boolean {
    return session.accountTransactions(this).value.data()?.any { transaction ->
        transaction.getConfirmations(session) == 0L
    } == true
}

fun String.getAssetNameOrNull(session: GdkSession?): String? {
    return if (session == null || this.isPolicyAsset(session)) {
        if (this == BTC_POLICY_ASSET) {
            "Bitcoin"
        } else {
            "Liquid Bitcoin"
        }.let {
            if (session?.isTestnet == true) "Testnet $it" else it
        }
    } else {
        session.liquid?.let { session.getAsset(this)?.name }
    }
}

@Deprecated("Use EnrichedAsset")
fun String.getAssetName(session: GdkSession): String {
    return getAssetNameOrNull(session) ?: this
}

@Deprecated("Use EnrichedAsset")
fun String.getAssetTicker(session: GdkSession): String? {
    return if (this.isPolicyAsset(session)) {
        if (this == BTC_POLICY_ASSET) {
            "BTC"
        } else {
            "LBTC"
        }.let {
            if (session.isTestnet) "TEST-$it" else it
        }
    } else {
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

fun List<String>.twoFactorMethodsLocalizedDeprecated(): List<String> = map {
    it.twoFactorMethodsLocalizedDeprecated()
}

suspend fun GdkSession.getWallet(database: Database, sessionManager: SessionManager): GreenWallet? {
    return (ephemeralWallet ?: (sessionManager.getWalletIdFromSession(this)?.let { walletId -> database.getWallet(walletId) }))
}