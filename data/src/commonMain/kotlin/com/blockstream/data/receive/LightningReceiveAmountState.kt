package com.blockstream.data.receive

sealed class LightningReceiveAmountState {
    data object None : LightningReceiveAmountState()
    data object Info : LightningReceiveAmountState()

    sealed class Error : LightningReceiveAmountState() {
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

    data class Recommend(val satsStr: String) : LightningReceiveAmountState()
}