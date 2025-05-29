package com.blockstream.green.database

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.objectId

fun roomToDelight(wallet: Wallet): com.blockstream.common.database.wallet.Wallet {
    return com.blockstream.common.database.wallet.Wallet(
        id = objectId(wallet.id).toString(),
        name = wallet.name,
        xpub_hash_id = wallet.walletHashId,
        ask_bip39_passphrase = wallet.askForBip39Passphrase,
        watch_only_username = wallet.watchOnlyUsername,
        is_recovery_confirmed = wallet.isRecoveryPhraseConfirmed,
        is_hardware = wallet.isHardware,
        is_testnet = wallet.isTestnet,
        is_lightning = wallet.isLightning,
        active_network = wallet.activeNetwork,
        active_account = wallet.activeAccount,
        device_identifiers = wallet.deviceIdentifiers,
        extras = null,
        order = wallet.order
    )
}

fun roomToDelight(wallet: GreenWallet, loginCredentials: LoginCredentials): com.blockstream.common.database.wallet.LoginCredentials {
    return com.blockstream.common.database.wallet.LoginCredentials(
        wallet_id = wallet.id,
        credential_type = loginCredentials.credentialType,
        network = loginCredentials.network,
        pin_data = loginCredentials.pinData,
        keystore = loginCredentials.keystore,
        encrypted_data = loginCredentials.encryptedData,
        counter = loginCredentials.counter
    )
}