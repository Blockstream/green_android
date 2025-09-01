package com.blockstream.green.data.meld.models

import kotlinx.serialization.Serializable

// These models are lean, the API returns a lot more data: https://docs.meld.io/reference/payments-transactions-search-1
@Serializable
data class MeldTransaction(
    val key: String,
    val id: String,
    val transactionType: String? = null,
    val status: String,
    val sourceAmount: Double? = null,
    val sourceCurrencyCode: String? = null,
    val destinationAmount: Double? = null,
    val destinationCurrencyCode: String? = null,
    val paymentMethodType: String? = null,
    val serviceProvider: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val countryCode: String? = null,
    val externalCustomerId: String? = null,
    val fiatAmountInUsd: Double? = null,
    val cryptoDetails: MeldCryptoDetails? = null
)

@Serializable
data class MeldCryptoDetails(
    val destinationWalletAddress: String? = null,
    val sessionWalletAddress: String? = null,
    val sourceWalletAddress: String? = null,
    val walletAddress: String? = null,
    val networkFee: Double? = null,
    val transactionFee: Double? = null,
    val partnerFee: Double? = null,
    val totalFee: Double? = null,
    val networkFeeInUsd: Double? = null,
    val transactionFeeInUsd: Double? = null,
    val partnerFeeInUsd: Double? = null,
    val totalFeeInUsd: Double? = null,
    val blockchainTransactionId: String? = null,
    val institution: String? = null,
    val chainId: String? = null
)