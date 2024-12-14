package com.blockstream.common.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.db.SqlDriver
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.toGreenWallet
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.Loggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext

const val DATABASE_NAME = "green.sqlite"

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): GreenDB {
    val driver = driverFactory.createDriver()
    val database = GreenDB(
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

class Database(driverFactory: DriverFactory, val settingsManager: SettingsManager) {

    private var db: GreenDB

    init {
        logger.d { "Init Database" }
        db = createDatabase(driverFactory)
    }

    private suspend fun <T> io(block: suspend CoroutineScope.() -> T): T {
        return withContext(context = Dispatchers.IO) {
            block()
        }
    }

    suspend fun getWallet(id: String): GreenWallet? = io {
        db.walletQueries.getWallet(id).executeAsOneOrNull()?.toGreenWallet()
    }

    fun getWalletFlow(id: String): Flow<GreenWallet> =
        db.walletQueries.getWallet(id).asFlow().mapNotNull {
            io {
                it.executeAsOneOrNull()?.toGreenWallet()
            }
        }

    fun getWalletFlowOrNull(id: String): Flow<GreenWallet?> =
        db.walletQueries.getWallet(id).asFlow().map {
            io {
                it.executeAsOneOrNull()?.toGreenWallet()
            }
        }

    suspend fun getMainnetWalletWithXpubHashId(
        xPubHashId: String,
    ): GreenWallet? = io {
        db.walletQueries.getMainnetWalletWithXpubHashId(
            xPubHashId
        ).executeAsOneOrNull()?.toGreenWallet()
    }

    suspend fun getWalletWithXpubHashId(
        xPubHashId: String,
        isTestnet: Boolean,
        isHardware: Boolean
    ): GreenWallet? = io {
        db.walletQueries.getWalletWithXpubHashId(
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
        db.walletQueries.getWalletWatchOnlyXpubHashId(
            xPubHashId,
            network,
            isHardware
        ).executeAsOneOrNull()?.toGreenWallet()
    }

    suspend fun insertWallet(greenWallet: GreenWallet) = io {
        val wallet = greenWallet.wallet
        db.walletQueries.insertWallet(
            id = wallet.id,
            name = wallet.name,
            xpub_hash_id = wallet.xpub_hash_id,
            active_network = wallet.active_network,
            active_account = wallet.active_account,
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
        db.walletQueries.updateWallet(
            name = wallet.name,
            xpub_hash_id = wallet.xpub_hash_id,
            active_network = wallet.active_network,
            active_account = wallet.active_account,
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
        db.walletQueries.deleteWallet(id)
    }

    suspend fun walletExists(xPubHashId: String, isHardware: Boolean): Boolean = io {
        db.walletQueries.walletExists(xpub_hash_id = xPubHashId, is_hardware = isHardware)
            .executeAsOne()
    }

    suspend fun walletsExists(): Boolean = io {
        db.walletQueries.walletsExists().executeAsOne()
    }

    fun walletsExistsFlow(): Flow<Boolean> = db.walletQueries.walletsExists().asFlow().map {
        io {
            it.executeAsOne()
        }
    }

    suspend fun getWallets(isHardware: Boolean) : List<GreenWallet> = io {
        db.walletQueries.getWallets(is_hardware = isHardware).executeAsList().map { it.toGreenWallet() }
    }

    fun getWalletsFlow(isHardware: Boolean) : Flow<List<GreenWallet>> =
        db.walletQueries.getWallets(is_hardware = isHardware).asFlow().map {
            io {
                it.executeAsList().map { it.toGreenWallet() }
            }
        }

    fun getWalletsFlow(credentialType: CredentialType, isHardware: Boolean) : Flow<List<GreenWallet>> =
        db.walletQueries.getWalletsWithCredentialType(credentialType, isHardware).asFlow().map {
            io {
                it.executeAsList().map { it.toGreenWallet() }
            }
        }

    suspend fun getAllWallets(): List<GreenWallet> = io {
        db.walletQueries.getAllWallets().executeAsList().map {
            it.toGreenWallet()
        }
    }

    fun getAllWalletsFlow(): Flow<List<GreenWallet>> = db.walletQueries.getAllWallets().asFlow().map {
        io {
            it.executeAsList().map {
                it.toGreenWallet()
            }
        }
    }

    suspend fun getLoginCredentials(id: String) = io {
        db.loginCredentialsQueries.getLoginCredentials(wallet_id = id).executeAsList()
    }

    suspend fun getLoginCredential(id: String, credentialType: CredentialType) = io {
        db.loginCredentialsQueries.getLoginCredential(wallet_id = id, credential_type = credentialType).executeAsOneOrNull()
    }

    fun getLoginCredentialsFlow(id: String) =
        db.loginCredentialsQueries.getLoginCredentials(wallet_id = id).asFlow().map {
            io {
                it.executeAsList()
            }
        }

    suspend fun replaceLoginCredential(loginCredentials: LoginCredentials) = io {
        db.loginCredentialsQueries.replaceLoginCredential(
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
        db.loginCredentialsQueries.deleteLoginCredentials(
            wallet_id = loginCredentials.wallet_id,
            credential_type = loginCredentials.credential_type
        )
    }

    suspend fun deleteLoginCredentials(walletId: String, type: CredentialType) = io {
        db.loginCredentialsQueries.deleteLoginCredentials(
            wallet_id = walletId,
            credential_type = type
        )
    }

    suspend fun insertEvent(eventId: String) = io {
        db.eventsQueries.insertEvent(
            id = eventId
        )
    }

    suspend fun eventExist(eventId: String) = io {
        db.eventsQueries.eventExists(
            id = eventId
        ).executeAsOne()
    }

    suspend fun deleteEvents() = io {
        db.eventsQueries.deleteEvents()
    }

    companion object : Loggable()
}