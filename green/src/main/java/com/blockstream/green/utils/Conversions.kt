package com.blockstream.green.utils

import com.blockstream.gdk.data.Balance
import com.blockstream.gdk.data.Settings
import com.blockstream.gdk.data.Transaction
import com.blockstream.gdk.params.Convert
import com.blockstream.green.gdk.GreenSession
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

fun getFiatCurrency(session: GreenSession): String{
    return session.getSettings()?.pricing?.currency ?: "N/A"
}

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

fun getNumberFormat(decimals: Int,
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
    return getNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = false).format(feePerByte) + " satoshi / vbyte"
}

fun Double.feeRateWithUnit(): String {
    return getNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = false).format(this) + " satoshi / vbyte"
}

fun CharSequence.parse(decimals: Int = 2): Number? = this.toString().parse(decimals)

fun String.parse(decimals: Int = 2): Number? {
    return try {
        getNumberFormat(decimals = decimals, withDecimalSeparator = true, withGrouping = false).parse(this)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Balance.fiat(withUnit: Boolean = true): String {
    return try {
        val value = fiat.toDouble()
        getNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = false).format(value)
    } catch (e: Exception) {
        "N.A."
    } + if (withUnit) " $fiatCurrency" else ""
}

fun Balance.btc(session: GreenSession, withUnit: Boolean = true): String {
    return this.btc(session.getSettings()?.unit ?: "BTC", withUnit)
}

private fun Balance.btc(unit: String, withUnit: Boolean = true): String {
    return try {
        val value = getValue(unit).toDouble()
        getNumberFormat(decimals = getDecimals(unit), withDecimalSeparator = false, withGrouping = false).format(value)

    } catch (e: Exception) {
        "N.A."
    } + if (withUnit) " ${unit}" else ""
}

fun Long.btc(settings: Settings, withUnit: Boolean = true): String {
    return try {
        getNumberFormat(decimals = getDecimals(settings.unit), withDecimalSeparator = false, withGrouping = false).format(this)
    } catch (e: Exception) {
        "N.A."
    } + if (withUnit) " ${settings.unit}" else ""
}

fun Balance.asset(withUnit: Boolean = true): String {
    return try {
        getNumberFormat(assetInfo?.precision ?: 0, withDecimalSeparator = false, withGrouping = false).format(assetValue?.toDouble() ?: satoshi)
    } catch (e: Exception) {
        "N.A."
    } + if (withUnit) " ${assetInfo?.ticker ?: ""}" else ""
}

fun Long.toBTCLook(session: GreenSession, withUnit: Boolean = true, withDirection: Transaction.Type? = null): String {
    val look = session.convertAmount(Convert(this)).btc(session, withUnit = withUnit)

    withDirection?.let {
        if(it == Transaction.Type.REDEPOSIT || it == Transaction.Type.OUT){
            return "-$look"
        }
    }

    return look
}

fun Long.toAssetLook(session: GreenSession, assetId: String, withUnit: Boolean = true, withDirection: Transaction.Type? = null): String {
    val look = session.convertAmount(Convert(this, session.getAsset(assetId))).asset(withUnit = withUnit)

    withDirection?.let {
        if(it == Transaction.Type.REDEPOSIT || it == Transaction.Type.OUT){
            return "-$look"
        }
    }

    return look
}

fun Date.format(): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(this)