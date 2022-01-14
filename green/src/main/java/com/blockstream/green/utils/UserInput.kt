package com.blockstream.green.utils

import com.blockstream.gdk.data.Asset
import com.blockstream.gdk.data.Balance
import com.blockstream.gdk.params.Convert
import com.blockstream.gdk.params.Limits
import com.blockstream.green.gdk.GreenSession
import java.text.DecimalFormat
import java.text.ParsePosition
import java.util.*

// Parse user input respecting user Locale and convert the value to GDK compatible value (US Locale)
data class UserInput constructor(val amount: String, val decimals: Int, val unitKey: String, val isFiat: Boolean, val asset: Asset?) {

    fun toLimit() = Limits.fromUnit(unitKey, amount)

    fun getBalance(session: GreenSession): Balance? {
        return if(amount.isNotBlank()){
            if (asset == null) {
                session.convertAmount(Convert.forUnit(unitKey, amount))
            } else {
                session.convertAmount(Convert.forAsset(asset, amount), isAsset = true)
            }
        }else{
            null
        }
    }


    companion object{

        @Throws
        private fun parse(session: GreenSession, input: String?, assetId: String? = null, isFiat: Boolean = false, locale: Locale = Locale.getDefault(), throws : Boolean = true): UserInput {
            val unitKey : String
            // Users Locale
            val userNumberFormat : DecimalFormat
            // GDK format
            val gdkNumberFormat : DecimalFormat

            val asset: Asset?

            when {
                session.policyAsset != assetId && !assetId.isNullOrBlank() -> {
                    asset = session.getAsset(assetId) ?: Asset.createEmpty(assetId)

                    unitKey = getUnit(session)
                    userNumberFormat = userNumberFormat(asset.precision, withDecimalSeparator = false, withGrouping = true, locale = locale)
                    gdkNumberFormat = gdkNumberFormat(asset.precision)
                }
                isFiat -> {
                    asset = null
                    unitKey = getFiatCurrency(session)
                    userNumberFormat = userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = true, locale = locale)
                    gdkNumberFormat = gdkNumberFormat(decimals = 2, withDecimalSeparator = true)
                }
                else -> {
                    asset = null
                    unitKey = getUnit(session)
                    userNumberFormat = userNumberFormat(getDecimals(unitKey), withDecimalSeparator = false, withGrouping = true, locale = locale)
                    gdkNumberFormat = gdkNumberFormat(getDecimals(unitKey))
                }
            }

            return try{
                val input = if(input.isNullOrBlank()) "" else input
                val position = ParsePosition(0)

                val parsed = userNumberFormat.parse(input, position)

                if (position.index != input.length) {
                    throw Exception("id_error_parsing")
                }

                UserInput(gdkNumberFormat.format(parsed), gdkNumberFormat.minimumFractionDigits, unitKey , isFiat, asset = asset)
            }catch (e: Exception){
                if(throws){
                    throw e
                }else{
                    UserInput("", gdkNumberFormat.minimumFractionDigits, unitKey , isFiat, asset = asset)
                }
            }
        }

        @Throws
        fun parseUserInput(session: GreenSession, input: String?, assetId: String? = null, isFiat: Boolean = false, locale: Locale = Locale.getDefault()): UserInput {
            return parse(session, input, assetId, isFiat, locale, throws = true)
        }

        fun parseUserInputSafe(session: GreenSession, input: String?, assetId: String? = null, isFiat: Boolean = false, locale: Locale = Locale.getDefault()): UserInput {
            return parse(session, input, assetId, isFiat, locale, throws = false)
        }
    }
}