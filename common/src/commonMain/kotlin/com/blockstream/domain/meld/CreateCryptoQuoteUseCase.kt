package com.blockstream.domain.meld

import com.blockstream.common.BTC_UNIT
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.GdkSession
import com.blockstream.green.data.meld.MeldRepository
import com.blockstream.green.data.meld.data.CryptoQuoteRequest
import com.blockstream.green.data.meld.data.QuoteResponse
import com.blockstream.green.network.NetworkResponse
import com.blockstream.green.network.dataOrNull


class CreateCryptoQuoteUseCase constructor(
    private val meldRepository: MeldRepository
) {
    suspend operator fun invoke(
        session: GdkSession,
        country: String,
        amount: String,
        enrichedAsset: EnrichedAsset,
        denomination: Denomination,
        greenWallet: GreenWallet? = null
    ): NetworkResponse<List<QuoteResponse>> {
        val sourceAmount: String

        if (!denomination.isFiat) {
            throw Exception("Denomination should be in Fiat")
        }

        val sourceCurrencyCode = denomination.denomination

        sourceAmount = amount.ifBlank {
            meldRepository.getCryptoLimits(
                fiatCurrency = sourceCurrencyCode
            ).dataOrNull()?.firstOrNull()?.defaultAmount?.toInt().toString()
        }

        val cryptoQuote = CryptoQuoteRequest(
            countryCode = country,
            sourceAmount = sourceAmount,
            sourceCurrencyCode = sourceCurrencyCode,
            destinationCurrencyCode = enrichedAsset.ticker(session = session)?.uppercase()
                ?: BTC_UNIT,
            // externalCustomerId = greenWallet?.xPubHashId // Disable it for now, to allow cache to work for all users
        )

        return when (val response = meldRepository.createCryptoQuote(cryptoQuote = cryptoQuote)) {
            is NetworkResponse.Success -> {
                val quotes =
                    response.data.quotes?.sortedByDescending { it.destinationAmount } ?: emptyList()
                NetworkResponse.Success(quotes)
            }

            is NetworkResponse.Error -> response
        }
    }
}