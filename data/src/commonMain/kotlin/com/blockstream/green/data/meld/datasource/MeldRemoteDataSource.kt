package com.blockstream.green.data.meld.datasource

import com.blockstream.green.data.meld.MeldHttpClient
import com.blockstream.green.data.meld.data.CryptoQuoteRequest
import com.blockstream.green.data.meld.data.CryptoWidget
import com.blockstream.green.data.meld.data.CryptoWidgetRequest
import com.blockstream.green.data.meld.data.LimitsResponse
import com.blockstream.green.data.meld.data.QuotesResponse
import com.blockstream.green.data.meld.data.Resources
import com.blockstream.green.data.meld.models.Country
import com.blockstream.green.network.NetworkResponse

class MeldRemoteDataSource(
    private val client: MeldHttpClient
) {
    suspend fun createCryptoQuote(cryptoQuote: CryptoQuoteRequest): NetworkResponse<QuotesResponse> {
        return client.post(Resources.Payments.Crypto.Quote(), cryptoQuote)
    }

    suspend fun createCryptoWidget(widgetRequest: CryptoWidgetRequest): NetworkResponse<CryptoWidget> {
        return client.post(Resources.Crypto.Session.Widget(), widgetRequest)
    }

    suspend fun getCryptoLimits(fiatCurrency: String = "USD"): NetworkResponse<List<LimitsResponse>> {
        return client.get(Resources.Payments.Crypto.Limits(fiatCurrency = fiatCurrency))
    }

    suspend fun getTransactions(externalCustomerId: String): NetworkResponse<LimitsResponse> {
        return client.get(Resources.Payments.Transactions(externalCustomerIds = externalCustomerId))
    }

    suspend fun getCountries(): NetworkResponse<List<Country>> {
        return client.get(Resources.ServiceProviders.Properties.Countries())
    }
}