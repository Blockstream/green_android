package com.blockstream.domain.meld

import com.blockstream.data.BTC_UNIT
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.network.NetworkResponse
import com.blockstream.network.dataOrNull

class CreateCryptoQuoteUseCase constructor(
    private val meldRepository: com.blockstream.data.meld.MeldRepository
) {
    suspend operator fun invoke(
        session: GdkSession,
        country: String,
        amount: String,
        enrichedAsset: EnrichedAsset,
        denomination: Denomination,
        greenWallet: GreenWallet? = null
    ): NetworkResponse<List<com.blockstream.data.meld.data.QuoteResponse>> {
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

        val cryptoQuote = _root_ide_package_.com.blockstream.data.meld.data.CryptoQuoteRequest(
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