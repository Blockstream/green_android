package com.blockstream.data.managers

import com.blockstream.data.CountlyBase
import com.blockstream.data.data.Promo
import com.blockstream.utils.Loggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PromoManager constructor(
    val settingsManager: SettingsManager,
    val countly: CountlyBase,
) {
    private val scope = CoroutineScope(context = Dispatchers.Default)

    private val _promos = MutableStateFlow<List<Promo>>(emptyList())
    val promos: StateFlow<List<Promo>> = _promos

    init {
        logger.d { "PromoManager init" }

        combine(
            merge(flowOf(Unit), countly.remoteConfigUpdateEvent),
            settingsManager.appSettingsStateFlow
        ) { _, appSettings ->
            if (!appSettings.tor) {
                updatePromos()
            }
        }.launchIn(scope)
    }

    private fun updatePromos() {
        _promos.value = countly.getRemoteConfigValueForPromosV2()
            .orEmpty()
            .filterNot { settingsManager.isPromoDismissed(it.id) }
    }

    fun dismissPromo(id: String) {
        settingsManager.dismissPromo(id)
        _promos.update { promos -> promos.filterNot { it.id == id } }
    }

    fun reload() {
        scope.launch {
            updatePromos()
        }
    }

    companion object : Loggable()
}
