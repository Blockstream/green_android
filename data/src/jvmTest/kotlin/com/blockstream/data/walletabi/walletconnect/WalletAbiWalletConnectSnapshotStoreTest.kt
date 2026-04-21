package com.blockstream.data.walletabi.walletconnect

import com.blockstream.data.database.Database
import com.blockstream.data.database.DriverFactory
import com.blockstream.data.managers.SettingsManager
import com.blockstream.data.managers.WalletSettingsManager
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.test.runTest
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletAbiWalletConnectSnapshotStoreTest {

    private val settingsManager = SettingsManager(
        settings = PreferencesSettings(
            Preferences.userRoot().node("walletAbiWalletConnectSnapshotStoreTest/${UUID.randomUUID()}")
        ),
        analyticsFeatureEnabled = true,
        lightningFeatureEnabled = true,
        storeRateEnabled = true,
    )
    private val walletSettingsManager = WalletSettingsManager(
        database = Database(
            driverFactory = DriverFactory(),
            settingsManager = settingsManager,
        )
    )
    private val store = WalletAbiWalletConnectSnapshotStore(walletSettingsManager)
    private val walletId = "wallet-id"
    private val snapshotJson = """{"version":1,"wallet_id":"wallet-id"}"""

    @Test
    fun save_round_trips_snapshot() = runTest {
        store.save(walletId, snapshotJson)

        assertEquals(snapshotJson, store.load(walletId))
    }

    @Test
    fun load_returns_null_when_snapshot_missing() = runTest {
        assertNull(store.load(walletId))
    }

    @Test
    fun clear_removes_snapshot() = runTest {
        store.save(walletId, snapshotJson)

        store.clear(walletId)

        assertNull(store.load(walletId))
    }
}
