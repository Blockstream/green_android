package com.blockstream.common.data

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_custom
import blockstream_green.common.generated.resources.id_high
import blockstream_green.common.generated.resources.id_low
import blockstream_green.common.generated.resources.id_medium

sealed class FeePriority(
    open val fee: String? = null,
    open val feeFiat: String? = null,
    open val feeRate: String? = null,
    open val error: String? = null,
    open val expectedConfirmationTime: String? = null
) {
    data class Custom constructor(
        val customFeeRate: Double = Double.NaN,
        override val fee: String? = null,
        override val feeFiat: String? = null,
        override val feeRate: String? = null,
        override val error: String? = null,
        override val expectedConfirmationTime: String? = null
    ) : FeePriority(
        fee = fee,
        feeFiat = feeFiat,
        error = error,
        expectedConfirmationTime = expectedConfirmationTime
    )

    data class Low(
        override val fee: String? = null,
        override val feeFiat: String? = null,
        override val feeRate: String? = null,
        override val error: String? = null,
        override val expectedConfirmationTime: String? = null
    ) :
        FeePriority(
            fee = fee,
            feeFiat = feeFiat,
            feeRate = feeRate,
            error = error,
            expectedConfirmationTime = expectedConfirmationTime
        )

    data class Medium(
        override val fee: String? = null,
        override val feeFiat: String? = null,
        override val feeRate: String? = null,
        override val error: String? = null,
        override val expectedConfirmationTime: String? = null
    ) :
        FeePriority(
            fee = fee,
            feeFiat = feeFiat,
            feeRate = feeRate,
            error = error,
            expectedConfirmationTime = expectedConfirmationTime
        )

    data class High(
        override val fee: String? = null,
        override val feeFiat: String? = null,
        override val feeRate: String? = null,
        override val error: String? = null,
        override val expectedConfirmationTime: String? = null
    ) :
        FeePriority(
            fee = fee,
            feeFiat = feeFiat,
            feeRate = feeRate,
            error = error,
            expectedConfirmationTime = expectedConfirmationTime
        )

    val index
        get() = when (this) {
            is Custom -> 0
            is Low -> 1
            is Medium -> 2
            is High -> 3
        }

    val title
        get() = when (this) {
            is Custom -> Res.string.id_custom
            is Low -> Res.string.id_low
            is Medium -> Res.string.id_medium
            is High -> Res.string.id_high
        }

    val enabled
        get() = error == null

    val isPrimitive: Boolean
        get() = fee == null && feeFiat == null && feeRate == null && error == null && expectedConfirmationTime == null

    // Useful to be used
    fun primitive() = when (this) {
        is Custom -> Custom(customFeeRate = customFeeRate)
        is High -> High()
        is Low -> Low()
        is Medium -> Medium()
    }

    companion object {
        fun fromIndex(index: Int): FeePriority {
            return when (index) {
                0 -> Custom(Double.NaN)
                1 -> Low()
                2 -> Medium()
                else -> High()

            }
        }
    }
}