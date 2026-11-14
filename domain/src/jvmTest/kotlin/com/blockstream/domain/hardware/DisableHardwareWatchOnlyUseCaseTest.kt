package com.blockstream.domain.hardware

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.EncryptedData
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.WalletExtras
import com.blockstream.data.database.Database
import com.blockstream.data.database.DriverFactory
import com.blockstream.data.extensions.createLoginCredentials
import com.blockstream.data.managers.SettingsManager
import com.blockstream.utils.LogBucket
import com.blockstream.utils.Loggable.Companion.COMBINED_LOG_QUALIFIER
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Runs [DisableHardwareWatchOnlyUseCase] against an in-memory wallet database: the hardware
 * watch-only credential has to go, other credentials have to stay, and the wallet has to record
 * that the feature is off so a later device login does not turn it back on.
 */
class DisableHardwareWatchOnlyUseCaseTest {

    // Database needs real settings; an isolated preferences node keeps the test off the user's own.
    private val preferences = Preferences.userRoot().node("com/blockstream/green/test/${UUID.randomUUID()}")

    private lateinit var database: Database
    private lateinit var useCase: DisableHardwareWatchOnlyUseCase

    @BeforeTest
    fun setUp() {
        // Loggable resolves its logger through Koin, so the database cannot be built without it.
        startKoin {
            modules(
                module {
                    factory<Logger>(named(COMBINED_LOG_QUALIFIER)) { (tag: String, _: LogBucket) ->
                        Logger(config = StaticConfig(logWriterList = emptyList()), tag = tag)
                    }
                }
            )
        }

        database = Database(
            driverFactory = DriverFactory(),
            settingsManager = SettingsManager(
                settings = PreferencesSettings(preferences),
                analyticsFeatureEnabled = false,
                lightningFeatureEnabled = false,
                storeRateEnabled = false
            )
        )
        useCase = DisableHardwareWatchOnlyUseCase(database)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        preferences.removeNode()
    }

    @Test
    fun removesTheCredentialAndKeepsTheOthers() = runTest {
        val wallet = storedHardwareWallet()
        database.replaceLoginCredential(credential(wallet, CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS))
        database.replaceLoginCredential(credential(wallet, CredentialType.KEYSTORE_LIGHTNING_MNEMONIC))

        useCase(wallet)

        assertNull(database.getLoginCredential(wallet.id, CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS))
        assertNotNull(database.getLoginCredential(wallet.id, CredentialType.KEYSTORE_LIGHTNING_MNEMONIC))
    }

    @Test
    fun recordsThatWatchOnlyIsOff() = runTest {
        val wallet = storedHardwareWallet()

        useCase(wallet)

        assertEquals(false, wallet.extras?.hwWatchOnlyEnabled)
        assertEquals(false, database.getWallet(wallet.id)?.extras?.hwWatchOnlyEnabled)
    }

    @Test
    fun keepsTheOtherExtras() = runTest {
        val wallet = storedHardwareWallet(extras = WalletExtras(totalBalanceInFiat = true, hwWatchOnlyEnabled = true))

        useCase(wallet)

        assertEquals(WalletExtras(totalBalanceInFiat = true, hwWatchOnlyEnabled = false), database.getWallet(wallet.id)?.extras)
    }

    @Test
    fun anEphemeralWalletIsRejected() = runTest {
        val wallet = GreenWallet.createEphemeralWallet(networkId = "electrum-liquid", isHardware = true)

        assertFailsWith<IllegalStateException> { useCase(wallet) }

        assertNull(wallet.extras?.hwWatchOnlyEnabled)
        assertNull(database.getWallet(wallet.id))
    }

    private suspend fun storedHardwareWallet(extras: WalletExtras? = WalletExtras(hwWatchOnlyEnabled = true)): GreenWallet =
        GreenWallet.createWallet(name = "Jade", isHardware = true, extras = extras).also {
            database.insertWallet(it)
            // Inserting a wallet does not store its extras; updating it does.
            database.updateWallet(it)
        }

    private fun credential(wallet: GreenWallet, type: CredentialType) = createLoginCredentials(
        walletId = wallet.id,
        network = "electrum-liquid",
        credentialType = type,
        encryptedData = EncryptedData("", "")
    )
}
