package com.blockstream.data.meld.data

import kotlinx.serialization.Serializable

@Serializable
data class QuoteResponse(
    val transactionType: String,
    val sourceAmount: String,
    val sourceAmountWithoutFees: String,
    val fiatAmountWithoutFees: String,
    val destinationAmountWithoutFees: String? = null,
    val sourceCurrencyCode: String,
    val countryCode: String,
    val totalFee: String,
    val networkFee: String? = null,
    val transactionFee: String,
    val destinationAmount: String,
    val destinationCurrencyCode: String,
    val exchangeRate: String,
    val paymentMethodType: String,
    val customerScore: String,
    val serviceProvider: String,
    val institutionName: String? = null,
    val lowKyc: Boolean? = null,
    val partnerFee: String? = null
) {
    fun toCryptoWidgetRequest(
        walletAddress: String,
        externalCustomerId: String? = null
    ): com.blockstream.data.meld.data.CryptoWidgetRequest {
        return _root_ide_package_.com.blockstream.data.meld.data.CryptoWidgetRequest(
            sessionType = "BUY",
            externalCustomerId = externalCustomerId,
            sessionData = _root_ide_package_.com.blockstream.data.meld.data.SessionData(
                countryCode = countryCode,
                sourceAmount = sourceAmount,
                sourceCurrencyCode = sourceCurrencyCode,
                destinationCurrencyCode = destinationCurrencyCode,
                walletAddress = walletAddress,
                serviceProvider = serviceProvider,
            )
        )
    }
}