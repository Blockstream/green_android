package com.blockstream.data.walletabi.flow

import com.blockstream.data.database.Database
import com.blockstream.data.database.DriverFactory
import com.blockstream.data.managers.SettingsManager
import com.blockstream.data.managers.WalletSettingsManager
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.test.runTest
import java.util.prefs.Preferences
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletAbiFlowSnapshotStoreTest {

    private val settingsManager = SettingsManager(
        settings = PreferencesSettings(
            Preferences.userRoot().node("walletAbiFlowSnapshotStoreTest/${UUID.randomUUID()}")
        ),
        analyticsFeatureEnabled = true,
        lightningFeatureEnabled = true,
        storeRateEnabled = true
    )
    private val walletSettingsManager = WalletSettingsManager(
        database = Database(
            driverFactory = DriverFactory(),
            settingsManager = settingsManager
        )
    )
    private val store = WalletAbiFlowSnapshotStore(walletSettingsManager)
    private val walletId = "wallet-id"
    private val snapshot = WalletAbiFlowSnapshotPayload(
        review = WalletAbiFlowReviewPayload(
            requestId = "request-id",
            walletId = walletId,
            title = "Review",
            message = "Message",
            accounts = listOf(
                WalletAbiAccountOptionPayload(
                    accountId = "account-id",
                    name = "Main"
                )
            ),
            selectedAccountId = "account-id",
            approvalTarget = WalletAbiApprovalTargetPayload(kind = "software")
        ),
        phase = "REQUEST_LOADED"
    )

    @Test
    fun save_round_trips_snapshot() = runTest {
        store.save(walletId, snapshot)

        assertEquals(snapshot, store.load(walletId))
    }

    @Test
    fun load_returns_null_when_snapshot_missing() = runTest {
        assertNull(store.load(walletId))
    }

    @Test
    fun clear_removes_snapshot() = runTest {
        store.save(walletId, snapshot)

        store.clear(walletId)

        assertNull(store.load(walletId))
    }
}
