package com.blockstream.domain.meld

class MeldUseCase constructor(
    val createCryptoQuoteUseCase: CreateCryptoQuoteUseCase,
    val createCryptoWidgetUseCase: CreateCryptoWidgetUseCase,
    val defaultValuesUseCase: DefaultValuesUseCase
)