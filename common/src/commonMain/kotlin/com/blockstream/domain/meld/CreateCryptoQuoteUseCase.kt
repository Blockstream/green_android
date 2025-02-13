package com.blockstream.domain.meld

import com.blockstream.common.BTC_UNIT
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.GdkSession
import com.blockstream.green.data.meld.MeldRepository
import com.blockstream.green.data.meld.data.CryptoQuoteRequest
import com.blockstream.green.data.meld.data.QuoteResponse


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
    ): List<QuoteResponse> {
        val sourceAmount: String

        if (!denomination.isFiat) {
            throw Exception("Denomination should be in Fiat")
        }

        val sourceCurrencyCode = denomination.denomination

        sourceAmount = amount.ifBlank {
            meldRepository.getCryptoLimits(
                fiatCurrency = sourceCurrencyCode
            ).defaultAmount?.toInt().toString()
        }

        val cryptoQuote = CryptoQuoteRequest(
            countryCode = country,
            sourceAmount = sourceAmount,
            sourceCurrencyCode = sourceCurrencyCode,
            destinationCurrencyCode = enrichedAsset.ticker(session = session)?.uppercase() ?: BTC_UNIT,
            // externalCustomerId = greenWallet?.xPubHashId // Disable it for now, to allow cache to work for all users
        )

        return meldRepository.createCryptoQuote(cryptoQuote = cryptoQuote).quotes ?: emptyList()
    }
}