package com.blockstream.compose.models.walletabi

import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.transaction.TransactionConfirmation

data class WalletAbiReviewOutputLook(
    val address: String,
    val amount: String,
    val amountFiat: String?,
    val assetId: String,
    val recipientScript: String?
)

data class WalletAbiReviewLook(
    val accountAssetBalance: AccountAssetBalance,
    val outputs: List<WalletAbiReviewOutputLook>,
    val recipientAddress: String,
    val amount: String,
    val amountFiat: String?,
    val assetName: String,
    val assetTicker: String?,
    val assetId: String,
    val networkName: String,
    val networkWireValue: String,
    val method: String,
    val abiVersion: String,
    val requestId: String,
    val broadcast: Boolean,
    val recipientScript: String?,
    val transactionConfirmation: TransactionConfirmation,
    val isRefreshing: Boolean = false
)
