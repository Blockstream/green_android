package com.blockstream.common.extensions

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.WalletIcon
import com.blockstream.common.data.toGreenWallet
import com.blockstream.common.database.Wallet
import com.blockstream.common.views.wallet.WalletListLook

fun previewWallet(isHardware: Boolean): GreenWallet {
    return Wallet(
        id = objectId().toString(),
        name = if(isHardware) listOf("Jade USB", "Jade BLE", "Ledger").random() else "Wallet #${(1L..999L).random()}",
        xpub_hash_id = "",
        ask_bip39_passphrase = false,
        watch_only_username = null,
        is_hardware = isHardware,
        is_testnet = false,
        is_lightning = false,
        active_network = "",
        active_account = 0,
        device_identifiers = null,
        extras = null,
        order = 0
    ).toGreenWallet()
}

fun previewWalletListView(isHardware: Boolean): WalletListLook {
    val wallet = previewWallet((isHardware))

    return WalletListLook(
        greenWallet = wallet,
        title = wallet.name,
        subtitle = if(wallet.isEphemeral) "Jade".takeIf { isHardware } else null,
        hasLightningShortcut = wallet.hasLightningShortcut,
        isConnected = false,
        isLightningShortcutConnected = false,
        icon = WalletIcon.REGULAR
    )
}