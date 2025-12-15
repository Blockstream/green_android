@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.compose.extensions

import com.blockstream.compose.looks.transaction.Completed
import com.blockstream.compose.looks.transaction.TransactionLook
import com.blockstream.compose.looks.transaction.TransactionStatus
import com.blockstream.compose.looks.wallet.WalletListLook
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.WalletSerializable
import com.blockstream.data.database.wallet.LoginCredentials
import com.blockstream.data.devices.ConnectionType
import com.blockstream.data.devices.DeviceBrand
import com.blockstream.data.devices.GreenDevice
import com.blockstream.data.devices.GreenDeviceImpl
import com.blockstream.data.extensions.objectId
import com.blockstream.data.gdk.Gdk
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.gdk.data.AccountBalance
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.AssetBalance
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.gdk.device.HardwareConnectInteraction
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun previewWallet(
    isHardware: Boolean = false,
    isWatchOnly: Boolean = false,
    isEphemeral: Boolean = false
): GreenWallet {
    return WalletSerializable(
        id = objectId().toString(),
        name = if (isHardware) listOf(
            "Jade USB",
            "Jade BLE",
            "Ledger"
        ).random() else "Wallet #${(1L..999L).random()}",
        xpub_hash_id = "",
        ask_bip39_passphrase = false,
        watch_only_username = if (isWatchOnly) "watch_only" else null,
        is_recovery_confirmed = true,
        is_testnet = false,
        is_hardware = isHardware,
        is_lightning = false,
        active_network = "",
        active_account = 0,
        device_identifiers = null,
        extras = null,
        order = 0
    ).let {
        GreenWallet(
            wallet = it,
            ephemeralIdOrNull = if (isEphemeral) 1 else null
        )
    }
}

fun previewNetwork(isMainnet: Boolean = true) =
    Network("mainnet", "Bitcoin", isMainnet, false, false)

fun previewWalletListView(
    isHardware: Boolean = false,
    isEphemeral: Boolean = false,
    isConnected: Boolean = false
): WalletListLook {
    val wallet = previewWallet(
        isHardware = isHardware,
        isEphemeral = isEphemeral,
    )

    return WalletListLook(
        greenWallet = wallet,
        title = wallet.name,
        subtitle = if (wallet.isEphemeral) "Jade".takeIf { isHardware }
            ?: wallet.ephemeralBip39Name else "",
        isWatchOnly = false,
        isConnected = isConnected,
        icon = wallet.icon
    )
}

fun previewLoginCredentials() =
    LoginCredentials("", CredentialType.BIOMETRICS_PINDATA, "", null, null, null, 0)

fun previewEnrichedAsset(isLiquid: Boolean = false) = if (isLiquid) EnrichedAsset.PreviewLBTC else EnrichedAsset.PreviewBTC

var _accountId = 0L
fun previewAccount(isLightning: Boolean = false) = Account(
    gdkName = "Account #$_accountId",
    pointer = _accountId++,
    type = if (isLightning) AccountType.LIGHTNING else AccountType.BIP84_SEGWIT,
    networkInjected = previewNetwork(),
    policyAsset = previewEnrichedAsset()
)

fun previewAccountBalance() =
    AccountBalance(previewAccount(), Denomination.BTC, "1 BTC", "150.000 USD")

fun previewAccountAsset(isLightning: Boolean = false) = AccountAsset(
    account = previewAccount(isLightning = isLightning),
    asset = EnrichedAsset.PreviewBTC
)

fun previewAssetBalance(isLiquid: Boolean = false) = AssetBalance(
    asset = if (isLiquid) EnrichedAsset.PreviewLBTC else EnrichedAsset.PreviewBTC,
    if (isLiquid) "1 LBTC" else "1 BTC", "150.000 USD"
)

fun previewAccountAssetBalance() = AccountAssetBalance(
    account = previewAccount(),
    asset = EnrichedAsset.PreviewBTC,
    Denomination.BTC, "1 BTC", "150.000 USD"
)

fun previewTransaction() = Transaction(
    blockHeight = 123,
    canRBF = true,
    createdAtTs = Clock.System.now().toEpochMilliseconds() * 1000,
    inputs = listOf(),
    outputs = listOf(),
    fee = 0,
    feeRate = 0,
    memo = "",
    spvVerified = "",
    txHash = Uuid.random().toString(),
    type = "",
    satoshi = mapOf(),
).also {
    it.accountInjected = previewAccount()
}

fun previewTransactionLook(status: TransactionStatus = Completed()) =
    TransactionLook(status, previewTransaction(), listOf("12311.123 BTC"), "1311.123 USD")

fun previewGreenDevice(isJade: Boolean = true) = object :
    GreenDeviceImpl(deviceBrand = if (isJade) DeviceBrand.Blockstream else DeviceBrand.Trezor, type = ConnectionType.USB, isBonded = true) {
    override val connectionIdentifier: String = ""
    override val uniqueIdentifier: String = ""
    override val name: String = if (isJade) "Jade" else "Trezor T"
    override val manufacturer: String = if (isJade) "Blockstream" else "Trezor"
    override val isOffline: Boolean = false
    override fun disconnect() {}
    override suspend fun getOperatingNetworkForEnviroment(
        greenDevice: GreenDevice,
        gdk: Gdk,
        isTestnet: Boolean
    ): Network? {
        TODO("Not yet implemented")
    }

    override suspend fun getOperatingNetwork(
        greenDevice: GreenDevice,
        gdk: Gdk,
        interaction: HardwareConnectInteraction
    ): Network? {
        TODO("Not yet implemented")
    }
}