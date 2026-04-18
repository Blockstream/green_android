package com.blockstream.domain.walletabi.flow

import com.blockstream.data.database.Database
import com.blockstream.data.database.DriverFactory
import com.blockstream.data.managers.SettingsManager
import com.blockstream.data.managers.WalletSettingsManager
import com.blockstream.data.walletabi.flow.WalletAbiFlowSnapshotStore
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.test.runTest
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletAbiFlowSnapshotRepositoryTest {

    private val settingsManager = SettingsManager(
        settings = PreferencesSettings(
            Preferences.userRoot().node("walletAbiFlowSnapshotRepositoryTest/${UUID.randomUUID()}")
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
    private val repository = WalletAbiFlowSnapshotRepository(
        store = WalletAbiFlowSnapshotStore(walletSettingsManager)
    )
    private val walletId = "wallet-id"
    private val snapshot = WalletAbiResumeSnapshot(
        review = WalletAbiFlowReview(
            requestContext = WalletAbiStartRequestContext(
                requestId = "request-id",
                walletId = walletId
            ),
            title = "Review",
            message = "Message",
            accounts = listOf(
                WalletAbiAccountOption(
                    accountId = "account-id",
                    name = "Main"
                )
            ),
            selectedAccountId = "account-id",
            approvalTarget = WalletAbiApprovalTarget.Jade(
                deviceName = "Jade",
                deviceId = "jade-id"
            )
        ),
        phase = WalletAbiResumePhase.AWAITING_APPROVAL,
        jade = WalletAbiJadeContext(
            deviceId = "jade-id",
            step = WalletAbiJadeStep.REVIEW,
            message = "Review on Jade",
            retryable = true
        )
    )

    @Test
    fun save_round_trips_snapshot() = runTest {
        repository.save(walletId, snapshot)

        assertEquals(snapshot, repository.load(walletId))
    }

    @Test
    fun load_returns_null_when_snapshot_missing() = runTest {
        assertNull(repository.load(walletId))
    }

    @Test
    fun clear_removes_snapshot() = runTest {
        repository.save(walletId, snapshot)

        repository.clear(walletId)

        assertNull(repository.load(walletId))
    }
}
