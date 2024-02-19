package com.blockstream.common.extensions

import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.database.Wallet
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.looks.wallet.WalletListLook

fun previewWallet(isHardware: Boolean = false, isWatchOnly: Boolean = false, isEphemeral: Boolean = false, hasLightningShortcut: Boolean = false): GreenWallet {
    return Wallet(
        id = objectId().toString(),
        name = if(isHardware) listOf("Jade USB", "Jade BLE", "Ledger").random() else "Wallet #${(1L..999L).random()}",
        xpub_hash_id = "",
        ask_bip39_passphrase = false,
        watch_only_username = if(isWatchOnly) "watch_only" else null,
        is_hardware = isHardware,
        is_testnet = false,
        is_lightning = false,
        active_network = "",
        active_account = 0,
        device_identifiers = null,
        extras = null,
        order = 0
    ).let {
        GreenWallet(wallet = it, ephemeralIdOrNull = if(isEphemeral) 1 else null, hasLightningShortcut = hasLightningShortcut)
    }
}

fun previewNetwork(isMainnet: Boolean = true) = Network("mainnet", "Bitcoin", "mainet", isMainnet, false, false)

fun previewWalletListView(isHardware: Boolean = false, isEphemeral: Boolean = false, hasLightningShortcut: Boolean = false): WalletListLook {
    val wallet = previewWallet(isHardware = isHardware, isEphemeral = isEphemeral, hasLightningShortcut = hasLightningShortcut)

    return WalletListLook(
        greenWallet = wallet,
        title = wallet.name,
        subtitle = if(wallet.isEphemeral) "Jade".takeIf { isHardware } ?: wallet.ephemeralBip39Name else null,
        hasLightningShortcut = wallet.hasLightningShortcut,
        isConnected = false,
        isLightningShortcutConnected = false,
        icon = wallet.icon
    )
}

fun previewLoginCredentials() = LoginCredentials("", CredentialType.BIOMETRICS_PINDATA, "", null, null, null, 0)


fun previewAccountAsset() = AccountAsset(
    account = Account(gdkName = "Segwit", pointer = 0, type = AccountType.BIP84_SEGWIT),
    asset = EnrichedAsset.PreviewBTC
)