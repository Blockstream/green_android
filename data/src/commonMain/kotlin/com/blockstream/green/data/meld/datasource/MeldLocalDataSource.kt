package com.blockstream.green.data.meld.datasource

import com.blockstream.green.data.meld.models.Country
import com.blockstream.green.data.meld.models.MeldTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MeldLocalDataSource {
    private var cachedCountries: List<Country>? = null
    
    private val transactionCache = mutableMapOf<String, MutableStateFlow<List<MeldTransaction>>>()
    
    fun getCachedCountries(): List<Country>? {
        return cachedCountries
    }
    
    fun saveCountries(countries: List<Country>) {
        cachedCountries = countries
    }
    
    fun getTransactionFlow(externalCustomerId: String): StateFlow<List<MeldTransaction>> {
        return transactionCache.getOrPut(externalCustomerId) {
            MutableStateFlow(emptyList())
        }.asStateFlow()
    }
    
    fun updateTransactions(externalCustomerId: String, transactions: List<MeldTransaction>) {
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