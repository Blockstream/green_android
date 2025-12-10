package com.blockstream.common.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.db.SqlDriver
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.toGreenWallet
import com.blockstream.common.database.local.LocalDB
import com.blockstream.common.database.wallet.LoginCredentials
import com.blockstream.common.database.wallet.Wallet
import com.blockstream.common.database.wallet.WalletDB
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.getSecureRandom
import com.blockstream.green.utils.Loggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext

const val DATABASE_NAME_WALLET = "green.sqlite"
const val DATABASE_NAME_LOCAL = "local.sqlite"

expect class DriverFactory {
    fun createWalletDriver(): SqlDriver
    fun createLocalDriver(): SqlDriver
}

fun createWalletDatabase(driverFactory: DriverFactory): WalletDB {
    val driver = driverFactory.createWalletDriver()
    val database = WalletDB(
        driver = driver,
        loginCredentialsAdapter = LoginCredentials.Adapter(
            credential_typeAdapter = credentialsTypeAdapter,
            pin_dataAdapter = pinDataAdapter,
            encrypted_dataAdapter = encryptedDataAdapter
        ),
        walletAdapter = Wallet.Adapter(device_identifiersAdapter = deviceIdentifierAdapter, extrasAdapter = walletExtrasTypeAdapter)
    )

    return database
}

fun createLocalDatabase(driverFactory: DriverFactory): LocalDB {
    val driver = driverFactory.createLocalDriver()
    val database = LocalDB(
        driver = driver,
    )

    return database
}

//TODO: Refactor this into two separate database classes, local and remote.
class Database(driverFactory: DriverFactory, val settingsManager: SettingsManager) {

    private var walletDB: WalletDB
    private var localDB: LocalDB

    init {
        logger.d { "Init Databases" }
        walletDB = createWalletDatabase(driverFactory)
        localDB = createLocalDatabase(driverFactory)
    }

    private suspend fun <T> io(block: suspend CoroutineScope.() -> T): T {
        return withContext(context = Dispatchers.IO) {
            block()
        }
    }

    suspend fun getWallet(id: String): GreenWallet? = io {
        walletDB.walletQueries.getWallet(id).executeAsOneOrNull()?.toGreenWallet()
    }

    fun getWalletFlow(id: String): Flow<GreenWallet> =
        walletDB.walletQueries.getWallet(id).asFlow().mapNotNull {
            io {
                it.executeAsOneOrNull()?.toGreenWallet()
            }
        }

    fun getWalletFlowOrNull(id: String): Flow<GreenWallet?> =
        walletDB.walletQueries.getWallet(id).asFlow().map {
            io {
                it.executeAsOneOrNull()?.toGreenWallet()
            }
        }

    suspend fun getMainnetWalletWithXpubHashId(
        xPubHashId: String,
    ): GreenWallet? = io {
        walletDB.walletQueries.getMainnetWalletWithXpubHashId(
            xPubHashId
        ).executeAsOneOrNull()?.toGreenWallet()
    }

    suspend fun getWalletWithXpubHashId(
        xPubHashId: String,
        isTestnet: Boolean,
        isHardware: Boolean
    ): GreenWallet? = io {
        walletDB.walletQueries.getWalletWithXpubHashId(
            xPubHashId,
            isTestnet,
            isHardware
        ).executeAsOneOrNull()?.toGreenWallet()
    }

    suspend fun getWalletWatchOnlyXpubHashId(
        xPubHashId: String,
        network: String,
        isHardware: Boolean
    ): GreenWallet? = io {
        walletDB.walletQueries.getWalletWatchOnlyXpubHashId(
            xPubHashId,
            network,
            isHardware
        ).executeAsOneOrNull()?.toGreenWallet()
    }

    suspend fun insertWallet(greenWallet: GreenWallet) = io {
        val wallet = greenWallet.wallet
        walletDB.walletQueries.insertWallet(
            id = wallet.id,
            name = wallet.name,
            xpub_hash_id = wallet.xpub_hash_id,
            active_network = wallet.active_network,
            active_account = wallet.active_account,
            is_recovery_confirmed = wallet.is_recovery_confirmed,
            is_testnet = wallet.is_testnet,
            is_hardware = wallet.is_hardware,
            is_lightning = wallet.is_lightning,
            ask_bip39_passphrase = wallet.ask_bip39_passphrase,
            watch_only_username = wallet.watch_only_username,
            device_identifiers = wallet.device_identifiers,
            order = wallet.order
        )
        settingsManager.increaseWalletCounter()
    }

    suspend fun updateWallet(greenWallet: GreenWallet) = io {
        val wallet = greenWallet.wallet
        walletDB.walletQueries.updateWallet(
            name = wallet.name,
            xpub_hash_id = wallet.xpub_hash_id,
            active_network = wallet.active_network,
            active_account = wallet.active_account,
            is_recovery_confirmed = wallet.is_recovery_confirmed,
            is_testnet = wallet.is_testnet,
            is_hardware = wallet.is_hardware,
            is_lightning = wallet.is_lightning,
            ask_bip39_passphrase = wallet.ask_bip39_passphrase,
            watch_only_username = wallet.watch_only_username,
            device_identifiers = wallet.device_identifiers,
            extras = wallet.extras,
            order = wallet.order,
            id = wallet.id
        )
    }

    suspend fun deleteWallet(id: String) = io {
        walletDB.walletQueries.deleteWallet(id)
    }

    suspend fun walletExists(xPubHashId: String, isHardware: Boolean): Boolean = io {
        walletDB.walletQueries.walletExists(xpub_hash_id = xPubHashId, is_hardware = isHardware)
            .executeAsOne()
    }

    suspend fun walletsExists(): Boolean = io {
        walletDB.walletQueries.walletsExists().executeAsOne()
    }

    fun walletsExistsFlow(): Flow<Boolean> = walletDB.walletQueries.walletsExists().asFlow().map {
        io {
            it.executeAsOne()
        }
    }

    suspend fun getWallets(isHardware: Boolean): List<GreenWallet> = io {
        walletDB.walletQueries.getWallets(is_hardware = isHardware).executeAsList().map { it.toGreenWallet() }
    }

    fun getWalletsFlow(isHardware: Boolean): Flow<List<GreenWallet>> =
        walletDB.walletQueries.getWallets(is_hardware = isHardware).asFlow().map {
            io {
                it.executeAsList().map { it.toGreenWallet() }
            }
        }

    fun getWalletsFlow(credentialType: CredentialType, isHardware: Boolean): Flow<List<GreenWallet>> =
        walletDB.walletQueries.getWalletsWithCredentialType(credentialType, isHardware).asFlow().map {
            io {
                it.executeAsList().map { it.toGreenWallet() }
            }
        }

    suspend fun getAllWallets(): List<GreenWallet> = io {
        walletDB.walletQueries.getAllWallets().executeAsList().map {
            it.toGreenWallet()
        }
    }

    fun getAllWalletsFlow(): Flow<List<GreenWallet>> = walletDB.walletQueries.getAllWallets().asFlow().map {
        io {
            it.executeAsList().map {
                it.toGreenWallet()
            }
        }
    }

    suspend fun getLoginCredentials(id: String) = io {
        walletDB.loginCredentialsQueries.getLoginCredentials(wallet_id = id).executeAsList()
    }

    suspend fun getLoginCredential(id: String, credentialType: CredentialType) = io {
        walletDB.loginCredentialsQueries.getLoginCredential(wallet_id = id, credential_type = credentialType).executeAsOneOrNull()
    }

    fun getLoginCredentialFlow(id: String, credentialType: CredentialType) =
        walletDB.loginCredentialsQueries.getLoginCredential(
            wallet_id = id,
            credential_type = credentialType
        ).asFlow().map {
            io {
                it.executeAsOneOrNull()
            }
        }

    fun getLoginCredentialsFlow(id: String) =
        walletDB.loginCredentialsQueries.getLoginCredentials(wallet_id = id).asFlow().map {
            io {
                it.executeAsList()
            }
        }

    suspend fun replaceLoginCredential(loginCredentials: LoginCredentials) = io {
        walletDB.loginCredentialsQueries.replaceLoginCredential(
            wallet_id = loginCredentials.wallet_id,
            credential_type = loginCredentials.credential_type,
            network = loginCredentials.network,
            pin_data = loginCredentials.pin_data,
            keystore = loginCredentials.keystore,
            encrypted_data = loginCredentials.encrypted_data,
            counter = loginCredentials.counter,
        )
    }

    suspend fun deleteLoginCredentials(loginCredentials: LoginCredentials) = io {
        walletDB.loginCredentialsQueries.deleteLoginCredentials(
            wallet_id = loginCredentials.wallet_id,
            credential_type = loginCredentials.credential_type
        )
    }

    suspend fun deleteLoginCredentials(walletId: String, type: CredentialType) = io {
        walletDB.loginCredentialsQueries.deleteLoginCredentials(
            wallet_id = walletId,
            credential_type = type
        )
    }

    suspend fun insertEvent(eventId: String, randomInsert: Boolean = false) = io {
        if (randomInsert && getSecureRandom().unsecureRandomInt(0, 100) > 90) {
            return@io
        }

        localDB.eventsQueries.insertEvent(
            id = eventId
        )
    }

    suspend fun eventExist(eventId: String) = io {
        localDB.eventsQueries.eventExists(
            id = eventId
        ).executeAsOne()
    }

    suspend fun deleteEvents() = io {
        localDB.eventsQueries.deleteEvents()
    }

    suspend fun getWalletSettings(walletId: String) = io {
        walletDB.walletSettingsQueries.getWalletSettings(wallet_id = walletId).executeAsList()
    }

    fun getWalletSettingsFlow(walletId: String) = walletDB.walletSettingsQueries.getWalletSettings(wallet_id = walletId).asFlow().map {
        io {
            it.executeAsList()
        }
    }

    suspend fun getWalletSetting(walletId: String, key: String) = io {
        walletDB.walletSettingsQueries.getWalletSetting(wallet_id = walletId, key = key).executeAsOneOrNull()
    }

    fun getWalletSettingFlow(walletId: String, key: String) =
        walletDB.walletSettingsQueries.getWalletSetting(wallet_id = walletId, key = key).asFlow().map {
            it.executeAsOneOrNull()
        }

    suspend fun setWalletSetting(walletId: String, key: String, data: String) = io {
        walletDB.walletSettingsQueries.setWalletSetting(wallet_id = walletId, key = key, data_ = data)
    }

    suspend fun deleteWalletSetting(walletId: String, key: String) = io {
        walletDB.walletSettingsQueries.deleteWalletSetting(wallet_id = walletId, key = key)
    }

    suspend fun getPendingSwaps(xPubHashId: String) = io {
        walletDB.boltzSwapsQueries.getPendingSwaps(xpub_hash_id = xPubHashId).executeAsList()
    }

    suspend fun getSwap(id: String) = io {
        walletDB.boltzSwapsQueries.getSwap(id = id).executeAsOneOrNull()
    }

    suspend fun getSwapFromTxHash(txHash: String) = io {
        walletDB.boltzSwapsQueries.getSwapFromTxHash(tx_hash = txHash).executeAsOneOrNull()
    }

    suspend fun getSwapFromInvoice(invoice: String, xPubHashId: String) = io {
        walletDB.boltzSwapsQueries.getSwapFromInvoice(invoice = invoice, xpub_hash_id = xPubHashId).executeAsOneOrNull()
    }

    fun getPendingSwapsFlow(xPubHashId: String) = walletDB.boltzSwapsQueries.getPendingSwaps(xpub_hash_id = xPubHashId).asFlow().map {
        io {
            it.executeAsList()
        }
    }

    suspend fun setSwap(id: String, walletId: String, xPubHashId: String, invoice: String? = null, data: String) = io {
        walletDB.boltzSwapsQueries.setSwap(id = id, wallet_id = walletId, xpub_hash_id = xPubHashId, invoice = invoice, data_ = data)
    }

    suspend fun setSwapComplete(id: String) = io {
        walletDB.boltzSwapsQueries.setSwapComplete(id = id)
    }

    suspend fun setSwapTxHash(id: String, txHash: String) = io {
        walletDB.boltzSwapsQueries.setSwapTxHash(id = id, tx_hash = txHash)
    }

    suspend fun deleteSwap(id: String) = io {
        walletDB.boltzSwapsQueries.deleteSwap(id = id)
    }

    suspend fun deleteAllSwaps() = io {
        walletDB.boltzSwapsQueries.deleteAllSwaps()
    }

    companion object : Loggable()
}
