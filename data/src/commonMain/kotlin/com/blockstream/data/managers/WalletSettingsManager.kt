package com.blockstream.data.managers

import com.blockstream.data.database.Database
import com.blockstream.data.database.wallet.WalletSettings
import com.blockstream.data.extensions.tryCatchNull
import com.blockstream.utils.Loggable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WalletSettingsManager(
    val database: Database
) {
    fun getWalletSettings(walletId: String): Flow<List<WalletSettings>> {
        return database.getWalletSettingsFlow(walletId = walletId)
    }

    suspend fun setLightningEnabled(walletId: String, enabled: Boolean) {
        setBoolean(walletId = walletId, key = KEY_LIGHTNING_ENABLED, enabled)
    }

    suspend fun isLightningEnabled(walletId: String): Boolean {
        return getBoolean(walletId = walletId, key = KEY_LIGHTNING_ENABLED)
    }

    suspend fun setLightningNodeId(walletId: String, nodeId: String) {
        setString(walletId = walletId, key = KEY_LIGHTNING_NODE_ID, nodeId)
    }

    suspend fun getLightningNodeId(walletId: String): String? {
        return getString(walletId = walletId, key = KEY_LIGHTNING_NODE_ID)
    }

    suspend fun setSwapsEnabled(walletId: String, enabled: Boolean) {
        setBoolean(walletId = walletId, key = KEY_SWAPS_ENABLED, enabled)
    }

    suspend fun isSwapsEnabled(walletId: String): Boolean {
        return getBoolean(walletId = walletId, key = KEY_SWAPS_ENABLED)
    }

    suspend fun setTotalBalanceInFiat(walletId: String, enabled: Boolean) {
        setBoolean(walletId = walletId, key = KEY_TOTAL_BALANCE_IN_FIAT, enabled)
    }

    suspend fun isTotalBalanceInFiat(walletId: String): Boolean {
        return getBoolean(walletId = walletId, key = KEY_TOTAL_BALANCE_IN_FIAT)
    }

    // Private methods
    private suspend fun getBoolean(walletId: String, key: String): Boolean {
        return database.getWalletSetting(walletId = walletId, key = key)?.let {
            it.data_ == TRUE_VALUE
        } ?: false
    }

    private fun getBooleanFlow(walletId: String, key: String): Flow<Boolean> {
        return database.getWalletSettingFlow(walletId = walletId, key = key).map {
            it?.data_ == TRUE_VALUE
        }
    }

    private suspend fun setBoolean(walletId: String, key: String, value: Boolean) {
        setString(walletId = walletId, key = key, if (value) TRUE_VALUE else FALSE_VALUE)
    }

    private suspend fun getLong(walletId: String, key: String): Long? {
        return getString(walletId = walletId, key = key)?.let {
            tryCatchNull { it.toLong() }
        }
    }

    private suspend fun setLong(walletId: String, key: String, value: Long) {
        setString(walletId = walletId, key = key, value.toString())
    }

    private suspend fun getString(walletId: String, key: String): String? {
        return database.getWalletSetting(walletId = walletId, key = key)?.data_
    }

    private suspend fun setString(walletId: String, key: String, value: String) {
        database.setWalletSetting(walletId = walletId, key = key, value)
    }

    companion object : Loggable() {
        const val TRUE_VALUE = "true"
        const val FALSE_VALUE = "false"
        const val KEY_LIGHTNING_ENABLED = "lightning_enabled"
        const val KEY_SWAPS_ENABLED = "swaps_enabled"
        const val KEY_LIGHTNING_NODE_ID = "lightning_node_id"
        const val KEY_TOTAL_BALANCE_IN_FIAT = "total_balance_in_fiat"
    }
}
