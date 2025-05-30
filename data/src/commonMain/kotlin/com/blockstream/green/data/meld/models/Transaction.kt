package com.blockstream.green.data.meld.models

import kotlinx.serialization.Serializable

// These models are lean, the API returns a lot more data: https://docs.meld.io/reference/payments-transactions-search-1
@Serializable
data class MeldTransaction(
    val key: String,
    val id: String,
    val transactionType: String,
    val status: String,
    val sourceAmount: Double,
    val sourceCurrencyCode: String,
    val destinationAmount: Double,
    val destinationCurrencyCode: String,
    val paymentMethodType: String,
    val serviceProvider: String,
    val serviceTransactionId: String,
    val createdAt: String,
    val updatedAt: String,
    val countryCode: String,
    val fiatAmountInUsd: Double,
    val cryptoDetails: MeldCryptoDetails? = null
)

@Serializable
data class MeldCryptoDetails(
    val walletAddress: String? = null,
    val networkFee: Double? = null,
    val transactionFee: Double? = null,
    val partnerFee: Double? = null,
    val totalFee: Double? = null,
    val blockchainTransactionId: String? = null,
    val chainId: String? = null
)