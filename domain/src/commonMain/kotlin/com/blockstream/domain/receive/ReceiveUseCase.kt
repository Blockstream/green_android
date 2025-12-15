package com.blockstream.domain.receive

class ReceiveUseCase constructor(
    val getReceiveAssetsUseCase: GetReceiveAssetsUseCase,
    val getReceiveAccountsUseCase: GetReceiveAccountsUseCase
)
