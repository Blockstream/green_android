package com.blockstream.data.meld.datasource

import com.blockstream.network.NetworkResponse

class MeldRemoteDataSource(
    private val client: com.blockstream.data.meld.MeldHttpClient
) {
    suspend fun createCryptoQuote(cryptoQuote: com.blockstream.data.meld.data.CryptoQuoteRequest): NetworkResponse<com.blockstream.data.meld.data.QuotesResponse> {
        return client.post(_root_ide_package_.com.blockstream.data.meld.data.Resources.Payments.Crypto.Quote(), cryptoQuote)
    }

    suspend fun createCryptoWidget(widgetRequest: com.blockstream.data.meld.data.CryptoWidgetRequest): NetworkResponse<com.blockstream.data.meld.data.CryptoWidget> {
        return client.post(_root_ide_package_.com.blockstream.data.meld.data.Resources.Crypto.Session.Widget(), widgetRequest)
    }

    suspend fun getCryptoLimits(fiatCurrency: String = "USD"): NetworkResponse<List<com.blockstream.data.meld.data.LimitsResponse>> {
        return client.get(_root_ide_package_.com.blockstream.data.meld.data.Resources.Payments.Crypto.Limits(fiatCurrency = fiatCurrency))
    }

    suspend fun getTransactions(
        externalCustomerId: String,
        statuses: List<com.blockstream.data.meld.data.MeldTransactionStatus> = emptyList()
    ): NetworkResponse<com.blockstream.data.meld.models.MeldTransactionResponse> {
        return client.get(
            _root_ide_package_.com.blockstream.data.meld.data.Resources.Payments.Transactions(
                externalCustomerIds = externalCustomerId,
                statuses = statuses.joinToString(",")
            )
        )
    }

    suspend fun getCountries(): NetworkResponse<List<com.blockstream.data.meld.models.Country>> {
        return client.get(_root_ide_package_.com.blockstream.data.meld.data.Resources.ServiceProviders.Properties.Countries())
    }
}