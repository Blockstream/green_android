package com.blockstream.green.utils

import com.blockstream.gdk.data.Balance
import com.blockstream.gdk.data.CreateTransaction
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.params.Convert
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.getAssetTicker
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.gdk.networkForAsset
import java.math.RoundingMode
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

const val BTC_UNIT = "BTC"

// Use it for GDK purposes
// Lowercase & replace μbtc -> ubtc
fun getUnit(network: Network, session: GdkSession) = session.getSettings(network)?.unit?.lowercase()
    ?.replace("\u00B5btc", "ubtc")
    ?: "btc"

// Use it for UI purposes
fun getFiatCurrency(network: Network, session: GdkSession): String{
    return session.getSettings(network)?.pricing?.currency?.getFiatUnit(network) ?: "n/a"
}

// Use it for UI purposes
fun String.getFiatUnit(network: Network): String {
    return if(network.isTestnet) "FIAT" else this
}

fun String.getFiatUnit(session: GdkSession): String {
    return if(session.isTestnet) "FIAT" else this
}

// Use it for UI purposes
@Deprecated("Use the AssetId")
fun getBitcoinOrLiquidUnit(network: Network, session: GdkSession, overrideDenomination: String? = null): String {
    var unit = overrideDenomination ?: session.getSettings(network)?.unit ?: "n/a"

    if (network.isTestnet) {
        unit = when (unit.lowercase()) {
            "btc" -> "TEST"
            "mbtc" -> "mTEST"
            "\u00B5btc" -> "\u00B5TEST"
            "bits" -> "bTEST"
            "sats" -> "sTEST"
            else -> unit
        }
    }

    return if (network.isLiquid) {
        "L-$unit"
    } else {
        unit
    }
}

fun getBitcoinOrLiquidUnit(assetId: String? = null, session: GdkSession, overrideDenomination: String? = null): String {
    val network = assetId.networkForAsset(session)
    var unit = overrideDenomination ?: session.getSettings(network)?.unit ?: "n/a"

    if (network.isTestnet) {
        unit = when (unit.lowercase()) {
            "btc" -> "TEST"
            "mbtc" -> "mTEST"
            "\u00B5btc" -> "\u00B5TEST"
            "bits" -> "bTEST"
            "sats" -> "sTEST"
            else -> unit
        }
    }

    return if (network.isLiquid) {
        "L-$unit"
    } else {
        unit
    }
}

fun getBitcoinOrLiquidSymbol(network: Network): String = if(network.isLiquid) "L-$BTC_UNIT" else BTC_UNIT

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

fun Balance?.toAmountLook(
    session: GdkSession,
    assetId: String? = null,
    isFiat: Boolean? = null,
    withUnit: Boolean = true,
    withGrouping: Boolean = true,
    withMinimumDigits: Boolean = false,
    overrideDenomination: Boolean = false,
): String? {
    if(this == null) return null
    return if(assetId.isPolicyAsset(session)){
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
                val unit = if (overrideDenomination) BTC_UNIT else session.getSettings(assetId.networkForAsset(session))?.unit ?: BTC_UNIT
                val value = getValue(unit).toDouble()
                userNumberFormat(
                    decimals = getDecimals(unit),
                    withDecimalSeparator = false,
                    withGrouping = withGrouping,
                    withMinimumDigits = withMinimumDigits,
                ).format(value).let {
                    if (withUnit) "$it ${getBitcoinOrLiquidUnit(assetId = assetId, session = session, overrideDenomination = unit)}" else it
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
                if (withUnit) "$it ${assetInfo?.ticker ?: assetId?.substring(0 until 10) ?: ""}" else it
            }
        } catch (e: Exception) {
            null
        }
    }
}

fun Long?.toAmountLookOrNa(
    session: GdkSession,
    assetId: String? = null,
    isFiat: Boolean? = null,
    withUnit: Boolean = true,
    withGrouping: Boolean = true,
    withDirection: Boolean = false,
    withMinimumDigits: Boolean = false,
    overrideDenomination: Boolean = false,
): String {
    if(this == null) return "n/a"
    return toAmountLook(session, assetId, isFiat, withUnit, withGrouping, withDirection, withMinimumDigits, overrideDenomination) ?: "n/a"
}

fun Long?.toAmountLook(
    session: GdkSession,
    assetId: String? = null,
    isFiat: Boolean? = null,
    withUnit: Boolean = true,
    withGrouping: Boolean = true,
    withDirection: Boolean = false,
    withMinimumDigits: Boolean = false,
    overrideDenomination: Boolean = false,
): String? {
    if(this == null) return null
    return if(assetId == null || assetId.isPolicyAsset(session)){
        if(isFiat == true) {
            session.convertAmount(assetId, Convert(satoshi = this))?.toAmountLook(
                session,
                assetId = assetId,
                isFiat = true,
                withUnit = withUnit,
                withGrouping = withGrouping,
                overrideDenomination = overrideDenomination
            )
        }else{
            session.convertAmount(assetId, Convert(satoshi = this))?.toAmountLook(
                session,
                assetId = assetId,
                withUnit = withUnit,
                withGrouping = withGrouping,
                withMinimumDigits = withMinimumDigits,
                overrideDenomination = overrideDenomination
            )
        }
    }else{
        if (isFiat == true) {
            null
        } else {
            // withMinimumDigits is not used on asset amounts
            session.convertAmount(
                assetId,
                Convert(satoshi = this, session.getAsset(assetId)),
                isAsset = true
            )?.toAmountLook(
                session = session,
                assetId = assetId,
                withUnit = withUnit,
                withGrouping = withGrouping,
                withMinimumDigits = withMinimumDigits,
                overrideDenomination = overrideDenomination
            )
        }
    }?.let { amount ->
        if(withDirection && this > 0L){
            "$amount"
        }else{
            amount
        }
    }
}

fun exchangeRate(session:GdkSession, assetId1: String, amount1: String, assetId2: String, amount2: String): String {
    val amount1Parsed = UserInput.parseUserInput(session = session, input = amount1, assetId = assetId1, isFiat = false)
    val amount2Parsed = UserInput.parseUserInput(session = session, input = amount2, assetId = assetId2, isFiat = false)

    val rate = amount2Parsed.amountAsDouble.toBigDecimal().divide(amount1Parsed.amountAsDouble.toBigDecimal(),8, RoundingMode.HALF_EVEN)

    val asset1Ticker = assetId1.getAssetTicker(session)
    val asset2Ticker = assetId2.getAssetTicker(session)

    return "1 $asset1Ticker = ${rate.toPlainString()} $asset2Ticker"
}
fun Date.formatOnlyDate(): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(this)

fun Date.formatWithTime(): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(this)

fun Date.formatAuto(): String =
    if ((this.time + 24 * 60 * 60 * 1000) > (System.currentTimeMillis())) {
        formatWithTime()
    } else {
        formatOnlyDate()
    }