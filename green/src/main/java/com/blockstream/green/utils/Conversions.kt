package com.blockstream.green.utils

import com.blockstream.gdk.data.Balance
import com.blockstream.gdk.data.CreateTransaction
import com.blockstream.gdk.data.Transaction
import com.blockstream.gdk.params.Convert
import com.blockstream.green.gdk.GreenSession
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

const val BTC_UNIT = "BTC"

// Use it for GDK purposes
// Lowercase & replace μbtc -> ubtc
fun getUnit(session: GreenSession) = session.getSettings()?.unit?.lowercase()
    ?.replace("\u00B5btc", "ubtc")
    ?: "btc"

// Use it for UI purposes
fun getFiatCurrency(session: GreenSession): String{
    return session.getSettings()?.pricing?.currency?.getFiatUnit(session) ?: "n/a"
}

// Use it for UI purposes
fun String.getFiatUnit(session: GreenSession ): String {
    return if(session.isTestnet) "FIAT" else this
}

// Use it for UI purposes
fun getBitcoinOrLiquidUnit(session: GreenSession, overrideDenomination: String? = null): String {
    var unit = overrideDenomination ?: session.getSettings()?.unit ?: "n/a"

    if (session.isTestnet) {
        unit = when (unit.lowercase()) {
            "btc" -> "TEST"
            "mbtc" -> "mTEST"
            "\u00B5btc" -> "\u00B5TEST"
            "bits" -> "bTEST"
            "sats" -> "sTEST"
            else -> unit
        }
    }

    return if (session.isLiquid) {
        "L-$unit"
    } else {
        unit
    }
}

fun getBitcoinOrLiquidSymbol(session: GreenSession): String = if(session.network.isLiquid) "L-$BTC_UNIT" else BTC_UNIT

fun getDecimals(unit: String): Int {
    return when (unit.lowercase()) {
        "btc" -> 8
        "mbtc" -> 5
        "ubtc", "bits", "\u00B5btc" -> 2
        else -> 0
    }
}

fun gdkNumberFormat(decimals: Int, withDecimalSeparator: Boolean = false) = (DecimalFormat.getInstance(Locale.US) as DecimalFormat).apply {
    minimumFractionDigits = if(withDecimalSeparator) decimals else 0
    maximumFractionDigits = decimals
    isGroupingUsed = false
    decimalFormatSymbols = DecimalFormatSymbols(Locale.US).also {
        it.decimalSeparator = '.'
        it.groupingSeparator = ',' // Unused
    }
}

fun userNumberFormat(decimals: Int,
                    withDecimalSeparator: Boolean,
                    withGrouping: Boolean = false,
                    withMinimumDigits:Boolean = false,
                    locale: Locale = Locale.getDefault()) = (DecimalFormat.getInstance(locale) as DecimalFormat).apply {
    minimumFractionDigits = if(withDecimalSeparator || withMinimumDigits) decimals else 0
    maximumFractionDigits = decimals
    isDecimalSeparatorAlwaysShown = withDecimalSeparator
    isGroupingUsed = withGrouping
}

fun CreateTransaction.feeRateWithUnit(): String? {
    return feeRate?.feeRateWithUnit()
}

fun Long.feeRateWithUnit(): String {
    val feePerByte = this / 1000.0
    return userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = true, withMinimumDigits = true).format(feePerByte).let {
        "$it satoshi / vbyte"
    }
}

fun Double.feeRateWithUnit(): String {
    return userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = false).format(this).let {
        "$it satoshi / vbyte"
    }
}

fun Balance?.toAmountLookOrNa(session: GreenSession, assetId: String? = null, isFiat: Boolean? = null, withUnit: Boolean = true, withGrouping: Boolean = true, withMinimumDigits: Boolean = false): String {
    if(this == null) return "n/a"
    return toAmountLook(session, assetId, isFiat, withUnit, withGrouping, withMinimumDigits) ?: "n/a"
}

fun Balance?.toAmountLook(
    session: GreenSession,
    assetId: String? = null,
    isFiat: Boolean? = null,
    withUnit: Boolean = true,
    withGrouping: Boolean = true,
    withMinimumDigits: Boolean = false,
    overrideDenomination: Boolean = false,
): String? {
    if(this == null) return null
    return if(assetId == null || assetId == session.policyAsset){
        if(isFiat == true) {
             try {
                 userNumberFormat(
                     decimals = 2,
                     withDecimalSeparator = true,
                     withGrouping = withGrouping
                 ).format(fiat?.toDouble())?.let {
                     if (withUnit) "$it ${fiatCurrency.getFiatUnit(session)}" else it
                 }
            } catch (e: Exception) {
                null
            }

        }else{
            try {
                val unit = if (overrideDenomination) BTC_UNIT else session.getSettings()?.unit ?: BTC_UNIT
                val value = getValue(unit).toDouble()
                userNumberFormat(
                    decimals = getDecimals(unit),
                    withDecimalSeparator = false,
                    withGrouping = withGrouping,
                    withMinimumDigits = withMinimumDigits,
                ).format(value).let {
                    if (withUnit) "$it ${getBitcoinOrLiquidUnit(session = session, overrideDenomination = unit)}" else it
                }
            } catch (e: Exception) {
                null
            }
        }

    }else{
        try {
            userNumberFormat(
                assetInfo?.precision ?: 0,
                withDecimalSeparator = false,
                withGrouping = withGrouping,
                withMinimumDigits = withMinimumDigits
            ).format(assetValue?.toDouble() ?: satoshi).let {
                if (withUnit) "$it ${assetInfo?.ticker ?: ""}" else it
            }
        } catch (e: Exception) {
            null
        }
    }
}

fun Long?.toAmountLookOrNa(
    session: GreenSession,
    assetId: String? = null,
    isFiat: Boolean? = null,
    withUnit: Boolean = true,
    withGrouping: Boolean = true,
    withDirection: Transaction.Type? = null,
    withMinimumDigits: Boolean = false,
    overrideDenomination: Boolean = false,
): String {
    if(this == null) return "n/a"
    return toAmountLook(session, assetId, isFiat, withUnit, withGrouping, withDirection, withMinimumDigits, overrideDenomination) ?: "n/a"
}

fun Long?.toAmountLook(
    session: GreenSession,
    assetId: String? = null,
    isFiat: Boolean? = null,
    withUnit: Boolean = true,
    withGrouping: Boolean = true,
    withDirection: Transaction.Type? = null,
    withMinimumDigits: Boolean = false,
    overrideDenomination: Boolean = false,
): String? {
    if(this == null) return null
    return if(assetId == null || assetId == session.policyAsset){
        if(isFiat == true) {
            session.convertAmount(Convert(satoshi = this))?.toAmountLook(
                session,
                isFiat = true,
                withUnit = withUnit,
                withGrouping = withGrouping,
                overrideDenomination = overrideDenomination
            )
        }else{
            session.convertAmount(Convert(satoshi = this))?.toAmountLook(
                session,
                withUnit = withUnit,
                withGrouping = withGrouping,
                withMinimumDigits = withMinimumDigits,
                overrideDenomination = overrideDenomination
            )
        }
    }else{
        // withMinimumDigits is not used on asset amounts
        session.convertAmount(Convert(satoshi = this, session.getAsset(assetId)), isAsset = true)?.toAmountLook(
            session = session,
            assetId = assetId,
            withUnit = withUnit,
            withGrouping = withGrouping,
            withMinimumDigits = withMinimumDigits,
            overrideDenomination = overrideDenomination
        )
    }?.let { amount ->
        withDirection?.let { direction ->
            if(direction == Transaction.Type.REDEPOSIT || direction == Transaction.Type.OUT){
                "-$amount"
            }else{
                "+$amount"
            }
        } ?: amount
    }
}

fun Date.formatOnlyDate(): String = DateFormat.getDateInstance(DateFormat.LONG).format(this)

fun Date.formatWithTime(): String = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(this)

fun Date.formatAuto(): String =
    if ((this.time + 24 * 60 * 60 * 1000) > (System.currentTimeMillis())) {
        formatWithTime()
    } else {
        formatOnlyDate()
    }