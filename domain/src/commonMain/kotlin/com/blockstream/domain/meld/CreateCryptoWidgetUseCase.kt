package com.blockstream.domain.meld

import com.blockstream.data.data.GreenWallet
import com.blockstream.network.NetworkResponse

class CreateCryptoWidgetUseCase(
    private val meldRepository: com.blockstream.data.meld.MeldRepository
) {
    suspend operator fun invoke(
        cryptoQuote: com.blockstream.data.meld.data.QuoteResponse,
        address: String,
        greenWallet: GreenWallet?
    ): NetworkResponse<com.blockstream.data.meld.data.CryptoWidget> {
        return meldRepository.createCryptoWidget(
            widgetRequest = cryptoQuote.toCryptoWidgetRequest(
                walletAddress = address,
                externalCustomerId = greenWallet?.xPubHashId
            )
        )
    }
}