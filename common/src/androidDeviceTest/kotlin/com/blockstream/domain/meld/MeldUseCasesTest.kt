package com.blockstream.domain.meld

import com.blockstream.BaseTest
import com.blockstream.common.data.Denomination
import com.blockstream.common.extensions.previewEnrichedAsset
import com.blockstream.common.managers.SessionManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MeldUseCasesTest : BaseTest() {

    @Test
    fun testQuotes() = runTest {
        val session = get<SessionManager>().getOnBoardingSession()

        val createCryptoQuoteUseCase: CreateCryptoQuoteUseCase = get()

        createCryptoQuoteUseCase.invoke(
            session = session,
            country = "US",
            enrichedAsset = previewEnrichedAsset(),
            amount = "5000",
            denomination = Denomination.FIAT("USD")
        ).first().also {
            assertEquals("5000", it.sourceAmount.toInt().toString())
        }

        createCryptoQuoteUseCase.invoke(
            session = session,
            country = "US",
            enrichedAsset = previewEnrichedAsset(),
            amount = "",
            denomination = Denomination.FIAT("USD")
        ).first().also {
            assert(it.sourceAmount.toInt() > 0)
        }
    }
}