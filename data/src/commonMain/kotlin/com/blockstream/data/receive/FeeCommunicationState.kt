package com.blockstream.data.receive

sealed class FeeCommunicationState {
    data object None : FeeCommunicationState()
    data object Info : FeeCommunicationState()

    sealed class Error : FeeCommunicationState() {
        data object InvalidAmount : Error()

        data class AmountTooHigh(
            val maxAmountStr: String,
            val maxFiatStr: String
        ) : Error()

        data class AmountTooLow(
            val minAmountStr: String,
            val minFiatStr: String
        ) : Error()
    }

    data class Recommend(val satsStr: String) : FeeCommunicationState()
}