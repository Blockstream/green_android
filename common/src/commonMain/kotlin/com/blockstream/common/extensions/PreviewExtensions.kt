package com.blockstream.common.extensions

import com.benasher44.uuid.uuid4
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.WalletSerializable
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.AccountBalance
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.looks.transaction.Completed
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.looks.wallet.WalletListLook
import kotlinx.datetime.Clock

fun previewWallet(
    isHardware: Boolean = false,
    isWatchOnly: Boolean = false,
    isEphemeral: Boolean = false,
    hasLightningShortcut: Boolean = false
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
        is_hardware = isHardware,
        is_testnet = false,
        is_lightning = false,
        active_network = "",
        active_account = 0,
        device_identifiers = null,
        extras = null,
        order = 0
    ).let {
        GreenWallet(
            wallet = it,
            ephemeralIdOrNull = if (isEphemeral) 1 else null,
            hasLightningShortcut = hasLightningShortcut
        )
    }
}

fun previewNetwork(isMainnet: Boolean = true) =
    Network("mainnet", "Bitcoin", "mainet", isMainnet, false, false)

fun previewWalletListView(
    isHardware: Boolean = false,
    isEphemeral: Boolean = false,
    hasLightningShortcut: Boolean = false
): WalletListLook {
    val wallet = previewWallet(
        isHardware = isHardware,
        isEphemeral = isEphemeral,
        hasLightningShortcut = hasLightningShortcut
    )

    return WalletListLook(
        greenWallet = wallet,
        title = wallet.name,
        subtitle = if (wallet.isEphemeral) "Jade".takeIf { isHardware }
            ?: wallet.ephemeralBip39Name else null,
        hasLightningShortcut = wallet.hasLightningShortcut,
        isConnected = false,
        isLightningShortcutConnected = false,
        icon = wallet.icon
    )
}

fun previewLoginCredentials() =
    LoginCredentials("", CredentialType.BIOMETRICS_PINDATA, "", null, null, null, 0)

fun previewEnrichedAsset(isLiquid: Boolean = false) = if(isLiquid) EnrichedAsset.PreviewLBTC else EnrichedAsset.PreviewBTC

var _accountId = 0L
fun previewAccount(isLightning:Boolean = false) = Account(
    gdkName = "Account #$_accountId",
    pointer = _accountId++,
    type = if(isLightning) AccountType.LIGHTNING else AccountType.BIP84_SEGWIT,
    networkInjected = previewNetwork(),
    policyAsset = previewEnrichedAsset()
)

fun previewAccountBalance() =
    AccountBalance(previewAccount(), Denomination.BTC, "1 BTC", "150.000 USD")

fun previewAccountAsset(isLightning : Boolean = false) = AccountAsset(
    account = previewAccount(isLightning = isLightning),
    asset = EnrichedAsset.PreviewBTC
)

fun previewAssetBalance() = AssetBalance(
    asset = EnrichedAsset.PreviewBTC,
    "1 BTC", "150.000 USD"
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
    txHash = uuid4().toString(),
    type = "",
    satoshi = mapOf(),
).also {
    it.accountInjected = previewAccount()
}

fun previewTransactionLook() = TransactionLook(Completed, 1, previewTransaction(), listOf("12311.123 BTC"))