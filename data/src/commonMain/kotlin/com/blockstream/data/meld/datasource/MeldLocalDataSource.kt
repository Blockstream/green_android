package com.blockstream.data.meld.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MeldLocalDataSource {
    private var cachedCountries: List<com.blockstream.data.meld.models.Country>? = null

    private val transactionCache = mutableMapOf<String, MutableStateFlow<List<com.blockstream.data.meld.models.MeldTransaction>>>()

    fun getCachedCountries(): List<com.blockstream.data.meld.models.Country>? {
        return cachedCountries
    }

    fun saveCountries(countries: List<com.blockstream.data.meld.models.Country>) {
        cachedCountries = countries
    }

    fun getTransactionFlow(externalCustomerId: String): StateFlow<List<com.blockstream.data.meld.models.MeldTransaction>> {
        return transactionCache.getOrPut(externalCustomerId) {
            MutableStateFlow(emptyList())
        }.asStateFlow()
    }

    fun updateTransactions(externalCustomerId: String, transactions: List<com.blockstream.data.meld.models.MeldTransaction>) {
        transactionCache.getOrPut(externalCustomerId) {
            MutableStateFlow(emptyList())
        }.value = transactions
    }
    
    fun getAllPendingWalletIds(): Set<String> {
        return transactionCache.keys.filter { customerId ->
            transactionCache[customerId]?.value?.isNotEmpty() == true
        }.toSet()
    }
}