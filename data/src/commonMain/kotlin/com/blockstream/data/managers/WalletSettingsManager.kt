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

    suspend fun setLightningNodeId(walletId: String, nodeId: String) {
        setString(walletId = walletId, key = KEY_LIGHTNING_NODE_ID, nodeId)
    }

    suspend fun getLightningNodeId(walletId: String): String? {
        return getString(walletId = walletId, key = KEY_LIGHTNING_NODE_ID)
    }

    suspend fun setTotalBalanceInFiat(walletId: String, enabled: Boolean) {
        setBoolean(walletId = walletId, key = KEY_TOTAL_BALANCE_IN_FIAT, enabled)
    }

    suspend fun isTotalBalanceInFiat(walletId: String): Boolean {
        return getBoolean(walletId = walletId, key = KEY_TOTAL_BALANCE_IN_FIAT)
    }

    suspend fun getWalletAbiFlowSnapshot(walletId: String): String? {
        return getString(walletId = walletId, key = KEY_WALLET_ABI_FLOW_SNAPSHOT)
    }

    fun observeWalletAbiFlowSnapshot(walletId: String): Flow<String?> {
        return getStringFlow(walletId = walletId, key = KEY_WALLET_ABI_FLOW_SNAPSHOT)
    }

    suspend fun setWalletAbiFlowSnapshot(walletId: String, snapshot: String) {
        setString(walletId = walletId, key = KEY_WALLET_ABI_FLOW_SNAPSHOT, value = snapshot)
    }

    suspend fun clearWalletAbiFlowSnapshot(walletId: String) {
        database.deleteWalletSetting(walletId = walletId, key = KEY_WALLET_ABI_FLOW_SNAPSHOT)
    }

    suspend fun getWalletAbiWalletConnectSnapshot(walletId: String): String? {
        return getString(walletId = walletId, key = KEY_WALLET_ABI_WALLETCONNECT_SNAPSHOT)
    }

    fun observeWalletAbiWalletConnectSnapshot(walletId: String): Flow<String?> {
        return getStringFlow(walletId = walletId, key = KEY_WALLET_ABI_WALLETCONNECT_SNAPSHOT)
    }

    suspend fun setWalletAbiWalletConnectSnapshot(walletId: String, snapshot: String) {
        setString(walletId = walletId, key = KEY_WALLET_ABI_WALLETCONNECT_SNAPSHOT, value = snapshot)
    }

    suspend fun clearWalletAbiWalletConnectSnapshot(walletId: String) {
        database.deleteWalletSetting(walletId = walletId, key = KEY_WALLET_ABI_WALLETCONNECT_SNAPSHOT)
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

    private fun getStringFlow(walletId: String, key: String): Flow<String?> {
        return database.getWalletSettingFlow(walletId = walletId, key = key).map { it?.data_ }
    }

    private suspend fun setString(walletId: String, key: String, value: String) {
        database.setWalletSetting(walletId = walletId, key = key, value)
    }

    companion object : Loggable() {
        const val TRUE_VALUE = "true"
        const val FALSE_VALUE = "false"
        const val KEY_LIGHTNING_NODE_ID = "lightning_node_id"
        const val KEY_TOTAL_BALANCE_IN_FIAT = "total_balance_in_fiat"
        const val KEY_WALLET_ABI_FLOW_SNAPSHOT = "wallet_abi_flow_snapshot"
        const val KEY_WALLET_ABI_WALLETCONNECT_SNAPSHOT = "wallet_abi_walletconnect_snapshot"
    }
}
