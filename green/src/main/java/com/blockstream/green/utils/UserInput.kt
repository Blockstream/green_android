package com.blockstream.green.utils

import com.blockstream.common.gdk.data.Asset
import com.blockstream.common.gdk.data.Balance
import com.blockstream.common.gdk.params.Convert
import com.blockstream.common.gdk.params.Limits
import com.blockstream.green.data.Denomination
import com.blockstream.green.extensions.isNotBlank
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.isPolicyAsset
import java.text.DecimalFormat
import java.text.ParsePosition
import java.util.Locale

// Parse user input respecting user Locale and convert the value to GDK compatible value (US Locale)
data class UserInput(
    val session: GdkSession,
    val amount: String,
    val amountAsDouble: Double,
    val denomination: Denomination,
    val assetId: String?,
    val asset: Asset?
) {
    fun toLimit() = Limits.fromUnit(denomination.denomination, amount)

    suspend fun getBalance(): Balance? {
        return if(amount.isNotBlank()){
            if (asset == null) {
                session.convertAmount(assetId, Convert.forUnit(denomination.denomination, amount))
            } else {
                session.convertAmount(assetId, Convert.forAsset(asset, amount), isAsset = true)
            }
        }else{
            null
        }
    }

    suspend fun toAmountLookOrEmpty(
        denomination: Denomination? = null,
        withUnit: Boolean = true,
        withGrouping: Boolean = true,
        withMinimumDigits: Boolean = false,
    ): String {
        return (getBalance()?.satoshi?.let {
            if (it > 0) {
                it.toAmountLook(
                    session = session,
                    assetId = assetId,
                    denomination = denomination ?: this.denomination,
                    withUnit = withUnit,
                    withGrouping = withGrouping,
                    withMinimumDigits = withMinimumDigits
                )
            } else {
                null
            }
        } ?: "")
    }

    companion object{

        @Throws
        private fun parse(
            session: GdkSession,
            input: String?,
            assetId: String? = null,
            denomination: Denomination? = null,
            locale: Locale = Locale.getDefault(),
            throws: Boolean = true,
        ): UserInput {
            val unitKey : String
            // Users Locale
            val userNumberFormat : DecimalFormat
            // GDK format
            val gdkNumberFormat : DecimalFormat

            val asset: Asset?

            return (denomination ?: Denomination.default(session)).let { denomination ->
                when {
                    !assetId.isPolicyAsset(session) -> { // Asset
                        asset = session.getAsset(assetId!!) ?: Asset.createEmpty(assetId)
                        userNumberFormat = userNumberFormat(asset.precision, withDecimalSeparator = false, withGrouping = true, locale = locale)
                        gdkNumberFormat = gdkNumberFormat(asset.precision)
                    }
                    denomination.isFiat -> { // Fiat
                        asset = null
                        userNumberFormat = userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = true, locale = locale)
                        gdkNumberFormat = gdkNumberFormat(decimals = 2, withDecimalSeparator = true)
                    }
                    else -> { // Policy Asset
                        asset = null
                        unitKey = denomination.denomination
                        userNumberFormat = userNumberFormat(getDecimals(unitKey), withDecimalSeparator = false, withGrouping = true, locale = locale)
                        gdkNumberFormat = gdkNumberFormat(getDecimals(unitKey))
                    }
                }

                try{
                    val input = if(input.isNullOrBlank()) "" else input
                    val position = ParsePosition(0)

                    val parsed = userNumberFormat.parse(input, position)

                    if (position.index != input.length) {
                        throw Exception("id_invalid_amount")
                    }

                    UserInput(session = session, amount = gdkNumberFormat.format(parsed), amountAsDouble = parsed.toDouble(), denomination = denomination, assetId = assetId, asset = asset)
                }catch (e: Exception){
                    if(throws){
                        throw e
                    }else{
                        UserInput(session = session, amount = "", amountAsDouble = 0.0, denomination = denomination, assetId = assetId, asset = asset)
                    }
                }
            }
        }

        @Throws
        fun parseUserInput(
            session: GdkSession,
            input: String?,
            denomination: Denomination? = null,
            assetId: String? = null,
            locale: Locale = Locale.getDefault()
        ): UserInput {
            return parse(session = session, input = input, denomination = denomination, assetId = assetId, locale = locale, throws = true)
        }

        fun parseUserInputSafe(
            session: GdkSession,
            input: String?,
            denomination: Denomination? = null,
            assetId: String? = null,
            locale: Locale = Locale.getDefault()
        ): UserInput {
            return parse(session = session, input = input, denomination = denomination, assetId = assetId, locale = locale, throws = false)
        }
    }
}