package com.blockstream.green.utils

import com.blockstream.gdk.data.Balance
import com.blockstream.gdk.data.Settings
import com.blockstream.gdk.data.Transaction
import com.blockstream.gdk.params.Convert
import com.blockstream.green.gdk.GreenSession
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun getFiatCurrency(session: GreenSession): String{
    return session.getSettings()?.pricing?.currency ?: "N/A"
}

// Use it for GDK purposes
// Lowercase & replace μbtc -> ubtc
fun getUnit(session: GreenSession) = session.getSettings()?.unit?.lowercase()
    ?.replace("\u00B5btc", "ubtc")
    ?: "btc"

// Use it for UI purposes
fun getBitcoinOrLiquidUnit(session: GreenSession): String{
    val unit = session.getSettings()?.unit ?: "N/A"
    if(session.network.isLiquid) {
        return "L-$unit"
    }
    return unit
}

fun getBitcoinOrLiquidSymbol(session: GreenSession): String = if(session.network.isLiquid) "L-BTC" else "BTC"

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
                    locale: Locale = Locale.getDefault()) = (DecimalFormat.getInstance(locale) as DecimalFormat).apply {
    minimumFractionDigits = if(withDecimalSeparator) decimals else 0
    maximumFractionDigits = decimals
    isDecimalSeparatorAlwaysShown = withDecimalSeparator
    isGroupingUsed = withGrouping
}

fun Long.feeRateWithUnit(): String {
    val feePerByte = this / 1000.0
    return userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = false).format(feePerByte) + " satoshi / vbyte"
}

fun Double.feeRateWithUnit(): String {
    return userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = false).format(this) + " satoshi / vbyte"
}

fun Balance.fiat(withUnit: Boolean = true): String {
    return try {
        val value = fiat.toDouble()
        userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = false).format(value)
    } catch (e: Exception) {
        "N.A."
    } + if (withUnit) " $fiatCurrency" else ""
}

fun Balance.btc(session: GreenSession, withUnit: Boolean = true, withGrouping: Boolean = false): String {
    return this.btc(unit = session.getSettings()?.unit ?: "BTC", withUnit = withUnit, withGrouping = withGrouping)
}

private fun Balance.btc(unit: String, withUnit: Boolean = true, withGrouping: Boolean = false): String {
    return try {
        val value = getValue(unit).toDouble()
        userNumberFormat(decimals = getDecimals(unit), withDecimalSeparator = false, withGrouping = withGrouping).format(value)
    } catch (e: Exception) {
        "N.A."
    } + if (withUnit) " ${unit}" else ""
}

fun Long.btc(settings: Settings, withUnit: Boolean = true): String {
    return try {
        userNumberFormat(decimals = getDecimals(settings.unit), withDecimalSeparator = false, withGrouping = false).format(this)
    } catch (e: Exception) {
        "N.A."
    } + if (withUnit) " ${settings.unit}" else ""
}

fun Balance.asset(withUnit: Boolean = true, withGrouping: Boolean = false): String {
    return try {
        userNumberFormat(assetInfo?.precision ?: 0, withDecimalSeparator = false, withGrouping = withGrouping).format(assetValue?.toDouble() ?: satoshi)
    } catch (e: Exception) {
        "N.A."
    } + if (withUnit) " ${assetInfo?.ticker ?: ""}" else ""
}

fun Long.toBTCLook(session: GreenSession, withUnit: Boolean = true, withDirection: Transaction.Type? = null, withGrouping: Boolean = false): String {
    val look = session.convertAmount(Convert(this)).btc(session, withUnit = withUnit, withGrouping = withGrouping)

    withDirection?.let {
        if(it == Transaction.Type.REDEPOSIT || it == Transaction.Type.OUT){
            return "-$look"
        }
    }

    return look
}

fun Long.toAssetLook(session: GreenSession, assetId: String, withUnit: Boolean = true, withGrouping: Boolean = false, withDirection: Transaction.Type? = null): String {
    val look = session.convertAmount(Convert(this, session.getAsset(assetId))).asset(withUnit = withUnit, withGrouping = withGrouping)

    withDirection?.let {
        if(it == Transaction.Type.REDEPOSIT || it == Transaction.Type.OUT){
            return "-$look"
        }
    }

    return look
}

fun Date.formatOnlyDate(): String = DateFormat.getDateInstance(DateFormat.LONG).format(this)

fun Date.formatWithTime(): String = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(this)

fun Date.formatAuto(): String =
    if ((this.time + 24 * 60 * 60 * 1000) > (System.currentTimeMillis())) {
        formatWithTime()
    } else {
        formatOnlyDate()
    }