package com.blockstream.green.data.meld

import com.blockstream.green.data.countries.Countries
import com.blockstream.green.data.meld.data.CryptoQuoteRequest
import com.blockstream.green.data.meld.data.CryptoWidget
import com.blockstream.green.data.meld.data.CryptoWidgetRequest
import com.blockstream.green.data.meld.data.LimitsResponse
import com.blockstream.green.data.meld.data.MeldTransactionStatus
import com.blockstream.green.data.meld.data.QuotesResponse
import com.blockstream.green.data.meld.datasource.MeldLocalDataSource
import com.blockstream.green.data.meld.datasource.MeldRemoteDataSource
import com.blockstream.green.data.meld.models.Country
import com.blockstream.green.data.meld.models.MeldTransaction
import com.blockstream.green.data.meld.models.MeldTransactionResponse
import com.blockstream.green.network.NetworkResponse
import kotlinx.coroutines.flow.StateFlow

class MeldRepository(
    private val remoteDataSource: MeldRemoteDataSource, private val localDataSource: MeldLocalDataSource
) {
    suspend fun createCryptoQuote(cryptoQuote: CryptoQuoteRequest): NetworkResponse<QuotesResponse> {
        return remoteDataSource.createCryptoQuote(cryptoQuote)
    }

    suspend fun createCryptoWidget(widgetRequest: CryptoWidgetRequest): NetworkResponse<CryptoWidget> {
        return remoteDataSource.createCryptoWidget(widgetRequest)
    }

    suspend fun getCryptoLimits(fiatCurrency: String = "USD"): NetworkResponse<List<LimitsResponse>> {
        return remoteDataSource.getCryptoLimits(fiatCurrency)
    }

    suspend fun getTransactions(
        externalCustomerId: String, statuses: List<MeldTransactionStatus> = emptyList()
    ): NetworkResponse<MeldTransactionResponse> {
        return remoteDataSource.getTransactions(externalCustomerId, statuses)
    }

    suspend fun getCountries(): NetworkResponse<List<Country>> {
        val cachedCountries = localDataSource.getCachedCountries()
        if (cachedCountries != null) {
            return NetworkResponse.Success(cachedCountries)
        }

        return when (val response = remoteDataSource.getCountries()) {
            is NetworkResponse.Success -> {
                val countriesWithEmojis = response.data.map { country ->
                    country.copy(flagEmoji = Countries.getEmojiFlagOrDefault(country.countryCode))
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
    
    fun getTransactionFlow(externalCustomerId: String): StateFlow<List<MeldTransaction>> {
        return localDataSource.getTransactionFlow(externalCustomerId)
    }
    
    fun updateTransactions(externalCustomerId: String, transactions: List<MeldTransaction>) {
        localDataSource.updateTransactions(externalCustomerId, transactions)
    }
}