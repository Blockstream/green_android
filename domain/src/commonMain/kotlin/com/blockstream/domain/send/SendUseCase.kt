package com.blockstream.domain.send

class SendUseCase constructor(
    val getSendAssetsUseCase: GetSendAssetsUseCase,
    val getSendAccountsUseCase: GetSendAccountsUseCase,
    val getSendAmountUseCase: GetSendAmountUseCase,
    val showFeeSelectorUseCase: ShowFeeSelectorUseCase,
    val getSendFlowUseCase: GetSendFlowUseCase,
    val prepareTransactionUseCase: PrepareTransactionUseCase,
    val getTransactionConfirmationUseCase: GetTransactionConfirmationUseCase,
)
