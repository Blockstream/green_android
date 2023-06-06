package com.blockstream.green.utils

import com.blockstream.common.BITS_UNIT
import com.blockstream.common.BTC_UNIT
import com.blockstream.common.MBTC_UNIT
import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.UBTC_UNIT
import com.blockstream.common.gdk.data.Balance
import com.blockstream.common.gdk.data.CreateTransaction
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.params.Convert
import com.blockstream.green.data.Denomination
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.getAssetTicker
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.gdk.networkForAsset
import java.math.RoundingMode
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Date
import java.util.Locale


// Use it for GDK purposes
// Lowercase & replace μbtc -> ubtc
fun getUnit(session: GdkSession) = session.getSettings()?.unit ?: BTC_UNIT

// Use it for UI purposes
fun getFiatCurrency(session: GdkSession): String{
    return session.getSettings()?.pricing?.currency?.getFiatUnit(session) ?: "n/a"
}

fun String.getFiatUnit(session: GdkSession): String {
    return if(session.isTestnet) "FIAT" else this
}

// TODO: Rename to networkUnit
fun getBitcoinOrLiquidUnit(
    session: GdkSession,
    assetId: String? = null,
    denomination: Denomination? = null
): String {
    val network = assetId.networkForAsset(session)
    var unit = denomination?.denomination ?: session.getSettings(network)?.unit ?: "n/a"

    if (network.isTestnet) {
        unit = when (unit) {
            BTC_UNIT -> "TEST"
            MBTC_UNIT -> "mTEST"
            UBTC_UNIT -> "\u00B5TEST"
            BITS_UNIT -> "bTEST"
            SATOSHI_UNIT -> "sTEST"
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
    return when (unit) {
        BTC_UNIT -> 8
        MBTC_UNIT -> 5
        BITS_UNIT, UBTC_UNIT -> 2
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
    if(this == null) return null
    return if(assetId.isPolicyAsset(session)){
        if(denomination?.isFiat == true) {
             try {
                 userNumberFormat(
                     decimals = 2,
                     withDecimalSeparator = true,
                     withGrouping = withGrouping
                 ).format(fiat?.toDouble())?.let {
                     if (withUnit) "$it ${fiatCurrency?.getFiatUnit(session)}" else it
                 }
            } catch (e: Exception) {
                null
            }

        }else{
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

suspend fun Long?.toAmountLookOrNa(
    session: GdkSession,
    assetId: String? = null,
    withUnit: Boolean = true,
    withGrouping: Boolean = true,
    withDirection: Boolean = false,
    withMinimumDigits: Boolean = false,
    denomination: Denomination? = null
): String {
    if(this == null) return "n/a"
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
    if(this == null) return null
    return if(assetId == null || assetId.isPolicyAsset(session)){
        if(denomination?.isFiat == true) {
            session.convertAmount(assetId, Convert(satoshi = this))?.toAmountLook(
                session,
                assetId = assetId,
                withUnit = withUnit,
                withGrouping = withGrouping,
                denomination = denomination
            )
        }else{
            session.convertAmount(assetId, Convert(satoshi = this))?.toAmountLook(
                session,
                assetId = assetId,
                withUnit = withUnit,
                withGrouping = withGrouping,
                withMinimumDigits = withMinimumDigits,
                denomination = denomination
            )
        }
    }else{
        if(denomination?.isFiat == true) {
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
                denomination = denomination
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
    val amount1Parsed = UserInput.parseUserInput(session = session, input = amount1, assetId = assetId1)
    val amount2Parsed = UserInput.parseUserInput(session = session, input = amount2, assetId = assetId2)

    val rate = amount2Parsed.amountAsDouble.toBigDecimal().divide(amount1Parsed.amountAsDouble.toBigDecimal(),8, RoundingMode.HALF_EVEN)

    val asset1Ticker = assetId1.getAssetTicker(session)
    val asset2Ticker = assetId2.getAssetTicker(session)

    return "1 $asset1Ticker = ${rate.toPlainString()} $asset2Ticker"
}
fun Date.formatMediumOnlyDate(): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(this)

fun Date.formatMediumWithTime(): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(this)

fun Date.formatFullWithTime(): String = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(this)

fun Date.formatAuto(): String =
    if ((this.time + 24 * 60 * 60 * 1000) > (System.currentTimeMillis())) {
        formatMediumWithTime()
    } else {
        formatMediumOnlyDate()
    }