package com.blockstream.common.utils

import com.blockstream.common.BITS_UNIT
import com.blockstream.common.BTC_UNIT
import com.blockstream.common.MBTC_UNIT
import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.UBTC_UNIT
import com.blockstream.common.data.Denomination
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.networkForAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Balance

// Use it for GDK purposes
// Lowercase & replace μbtc -> ubtc
fun getUnit(session: GdkSession) = session.getSettings()?.unit ?: BTC_UNIT

// Use it for UI purposes
fun getFiatCurrency(session: GdkSession): String {
    return session.getSettings()?.pricing?.currency?.getFiatUnit(session) ?: "n/a"
}

fun String.getFiatUnit(session: GdkSession): String {
    return if (session.isTestnet) "FIAT" else this
}

// TODO: Rename to networkUnit
fun getBitcoinOrLiquidUnit(
    session: GdkSession,
    assetId: String? = null,
    denomination: Denomination? = null
): String {
    val network = assetId.networkForAsset(session)
    var unit = denomination?.denomination ?: session.getSettings(network)?.unit ?: "n/a"

    if (network?.isTestnet == true) {
        unit = when (unit) {
            BTC_UNIT -> "TEST"
            MBTC_UNIT -> "mTEST"
            UBTC_UNIT -> "\u00B5TEST"
            BITS_UNIT -> "bTEST"
            SATOSHI_UNIT -> "sTEST"
            else -> unit
        }
    }

    return if (network?.isLiquid == true) {
        "L$unit"
    } else {
        unit
    }
}

fun getDecimals(unit: String): Int {
    return when (unit) {
        BTC_UNIT -> 8
        MBTC_UNIT -> 5
        BITS_UNIT, UBTC_UNIT -> 2
        else -> 0
    }
}

fun gdkNumberFormat(decimals: Int, withDecimalSeparator: Boolean = false) = (DecimalFormat(GDK_LOCALE)).apply {
    minimumFractionDigits = if (withDecimalSeparator) decimals else 0
    maximumFractionDigits = decimals
    isGroupingUsed = false
    decimalSeparator = '.'
    groupingSeparator = ','
}

fun userNumberFormat(
    decimals: Int,
    withDecimalSeparator: Boolean,
    withGrouping: Boolean = false,
    withMinimumDigits: Boolean = false,
    locale: String? = null
) = DecimalFormat(locale).apply {
    minimumFractionDigits = if (withDecimalSeparator || withMinimumDigits) decimals else 0
    maximumFractionDigits = decimals
    isDecimalSeparatorAlwaysShown = withDecimalSeparator
    isGroupingUsed = withGrouping
}

fun Long.feeRateWithUnit(): String {
    val feePerByte = this / 1000.0
    return userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = true, withMinimumDigits = true).format(feePerByte)
        .let {
            "$it sats / vbyte"
        }
}

fun Balance?.toAmountLook(
    session: GdkSession,
    assetId: String? = null,
    withUnit: Boolean = true,
    withGrouping: Boolean = true,
    withMinimumDigits: Boolean = false,
    denomination: Denomination? = null
): String? {
    if (this == null) return null
    return if (assetId.isPolicyAsset(session)) {
        if (denomination?.isFiat == true) {
            try {
                userNumberFormat(
                    decimals = 2,
                    withDecimalSeparator = true,
                    withGrouping = withGrouping
                ).format(fiat?.toDouble() ?: 0.0)?.let {
                    if (withUnit) "$it ${fiatCurrency?.getFiatUnit(session)}" else it
                }
            } catch (e: Exception) {
                null
            }

        } else {
            try {
                val unit = denomination?.denomination
                    ?: (session.getSettings(assetId.networkForAsset(session))?.unit ?: BTC_UNIT)

                val value = getValue(unit).toDouble()
                userNumberFormat(
                    decimals = getDecimals(unit),
                    withDecimalSeparator = false,
                    withGrouping = withGrouping,
                    withMinimumDigits = withMinimumDigits,
                ).format(value).let {
                    if (withUnit) "$it ${getBitcoinOrLiquidUnit(assetId = assetId, session = session, denomination = denomination)}" else it
                }
            } catch (e: Exception) {
                null
            }
        }

    } else {
        try {
            userNumberFormat(
                asset?.precision ?: 0,
                withDecimalSeparator = false,
                withGrouping = withGrouping,
                withMinimumDigits = withMinimumDigits
            ).format(assetAmount?.toDouble() ?: satoshi).let {
                if (withUnit) "$it ${asset?.ticker ?: assetId?.substring(0 until 6) ?: ""}" else it
            }
        } catch (e: Exception) {
            null
        }
    }
}

suspend fun Long?.toAmountLookOrNa(
    session: GdkSession,
    assetId: String? = null,
    withUnit: Boolean = true,
    withGrouping: Boolean = true,
    withDirection: Boolean = false,
    withMinimumDigits: Boolean = false,
    denomination: Denomination? = null
): String {
    if (this == null) return "n/a"
    return toAmountLook(
        session = session,
        assetId = assetId,
        withUnit = withUnit,
        withGrouping = withGrouping,
        withDirection = withDirection,
        withMinimumDigits = withMinimumDigits,
        denomination = denomination
    ) ?: "n/a"
}

suspend fun Long?.toAmountLook(
    session: GdkSession,
    assetId: String? = null,
    withUnit: Boolean = true,
    withGrouping: Boolean = true,
    withDirection: Boolean = false,
    withMinimumDigits: Boolean = false,
    denomination: Denomination? = null
): String? {
    if (this == null) return null
    val convert = session.convert(assetId = assetId, asLong = this)
    return if (assetId == null || assetId.isPolicyAsset(session)) {
        if (denomination?.isFiat == true) {
            convert?.toAmountLook(
                session,
                assetId = assetId,
                withUnit = withUnit,
                withGrouping = withGrouping,
                denomination = denomination
            )
        } else {
            convert?.toAmountLook(
                session,
                assetId = assetId,
                withUnit = withUnit,
                withGrouping = withGrouping,
                withMinimumDigits = withMinimumDigits,
                denomination = denomination
            )
        }
    } else {
        if (denomination?.isFiat == true) {
            null
        } else {
            // withMinimumDigits is not used on asset amounts
            convert?.toAmountLook(
                session = session,
                assetId = assetId,
                withUnit = withUnit,
                withGrouping = withGrouping,
                withMinimumDigits = withMinimumDigits,
                denomination = denomination
            )
        }
    }?.let { amount ->
        if (withDirection && this > 0L) {
            "$amount"
        } else {
            amount
        }
    }
}