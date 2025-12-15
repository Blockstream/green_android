package com.blockstream.data.meld

import com.blockstream.network.NetworkResponse
import kotlinx.coroutines.flow.StateFlow

class MeldRepository(
    private val remoteDataSource: com.blockstream.data.meld.datasource.MeldRemoteDataSource,
    private val localDataSource: com.blockstream.data.meld.datasource.MeldLocalDataSource
) {
    suspend fun createCryptoQuote(cryptoQuote: com.blockstream.data.meld.data.CryptoQuoteRequest): NetworkResponse<com.blockstream.data.meld.data.QuotesResponse> {
        return remoteDataSource.createCryptoQuote(cryptoQuote)
    }

    suspend fun createCryptoWidget(widgetRequest: com.blockstream.data.meld.data.CryptoWidgetRequest): NetworkResponse<com.blockstream.data.meld.data.CryptoWidget> {
        return remoteDataSource.createCryptoWidget(widgetRequest)
    }

    suspend fun getCryptoLimits(fiatCurrency: String = "USD"): NetworkResponse<List<com.blockstream.data.meld.data.LimitsResponse>> {
        return remoteDataSource.getCryptoLimits(fiatCurrency)
    }

    suspend fun getTransactions(
        externalCustomerId: String, statuses: List<com.blockstream.data.meld.data.MeldTransactionStatus> = emptyList()
    ): NetworkResponse<com.blockstream.data.meld.models.MeldTransactionResponse> {
        return remoteDataSource.getTransactions(externalCustomerId, statuses)
    }

    suspend fun getCountries(): NetworkResponse<List<com.blockstream.data.meld.models.Country>> {
        val cachedCountries = localDataSource.getCachedCountries()
        if (cachedCountries != null) {
            return NetworkResponse.Success(cachedCountries)
        }

        return when (val response = remoteDataSource.getCountries()) {
            is NetworkResponse.Success -> {
                val countriesWithEmojis = response.data.map { country ->
                    country.copy(flagEmoji = com.blockstream.data.countries.Countries.getEmojiFlagOrDefault(country.countryCode))
                }
                localDataSource.saveCountries(countriesWithEmojis)
                NetworkResponse.Success(countriesWithEmojis)
            }

            is NetworkResponse.Error -> response
        }
    }

    fun getAllPendingWalletIds(): Set<String> {
        return localDataSource.getAllPendingWalletIds()
    }

    fun getTransactionFlow(externalCustomerId: String): StateFlow<List<com.blockstream.data.meld.models.MeldTransaction>> {
        return localDataSource.getTransactionFlow(externalCustomerId)
    }

    fun updateTransactions(externalCustomerId: String, transactions: List<com.blockstream.data.meld.models.MeldTransaction>) {
        localDataSource.updateTransactions(externalCustomerId, transactions)
    }
}