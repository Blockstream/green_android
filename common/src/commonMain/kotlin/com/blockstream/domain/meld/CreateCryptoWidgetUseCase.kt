package com.blockstream.domain.meld

import com.blockstream.common.data.GreenWallet
import com.blockstream.green.data.meld.MeldRepository
import com.blockstream.green.data.meld.data.CryptoWidget
import com.blockstream.green.data.meld.data.QuoteResponse
import com.blockstream.green.network.NetworkResponse

class CreateCryptoWidgetUseCase constructor(
    private val meldRepository: MeldRepository
) {
    suspend operator fun invoke(
        cryptoQuote: QuoteResponse,
        address: String,
        greenWallet: GreenWallet?
    ): NetworkResponse<CryptoWidget> {
        return meldRepository.createCryptoWidget(
            widgetRequest = cryptoQuote.toCryptoWidgetRequest(
                walletAddress = address,
                externalCustomerId = greenWallet?.xPubHashId
            )
        )
    }
}