package com.blockstream.common

import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.gdk.data.Account
import kotlinx.coroutines.flow.SharedFlow

interface CountlyInteface {
    val remoteConfigUpdateEvent: SharedFlow<Unit>
    val isLightningFeatureEnabled: Boolean
    fun getRemoteConfigValueForAssets(key: String): Map<String, EnrichedAsset>?
    fun jadeInitialize()

    fun loginLightningStart()
    fun loginLightningStop()
    fun activeWalletStart()

    fun updateTorProxy(proxy: String)

    fun activeWalletEnd(
        session: Any,
        walletAssets: Map<String, Long>,
        accountAssets: Map<String, Map<String, Long>>,
        accounts: List<Account>
    )

    fun recordException(throwable: Throwable)
}