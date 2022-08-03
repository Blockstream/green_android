package com.blockstream.green.utils

import com.blockstream.gdk.data.Asset
import com.blockstream.gdk.data.Balance
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.params.Convert
import com.blockstream.gdk.params.Limits
import com.blockstream.green.extensions.isNotBlank
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.gdk.networkForAsset
import java.text.DecimalFormat
import java.text.ParsePosition
import java.util.*

// Parse user input respecting user Locale and convert the value to GDK compatible value (US Locale)
data class UserInput(
    val amount: String,
    val amountAsDouble: Double,
    val assetId: String?,
    val decimals: Int,
    val unitKey: String,
    val isFiat: Boolean,
    val asset: Asset?
) {

    fun toLimit() = Limits.fromUnit(unitKey, amount)

    fun getBalance(session: GdkSession): Balance? {
        return if(amount.isNotBlank()){
            if (asset == null) {
                session.convertAmount(assetId, Convert.forUnit(unitKey, amount))
            } else {
                session.convertAmount(assetId, Convert.forAsset(asset, amount), isAsset = true)
            }
        }else{
            null
        }
    }


    companion object{

        @Throws
        private fun parse(session: GdkSession, i: String?, assetId: String? = null, isFiat: Boolean = false, locale: Locale = Locale.getDefault(), throws : Boolean = true, networkForTest: Network? = null): UserInput {
            val unitKey : String
            // Users Locale
            val userNumberFormat : DecimalFormat
            // GDK format
            val gdkNumberFormat : DecimalFormat

            val asset: Asset?
            val network = networkForTest ?: assetId.networkForAsset(session)

            when {
                !assetId.isPolicyAsset(session) -> {
                    asset = session.getAsset(assetId!!) ?: Asset.createEmpty(assetId)

                    unitKey = getUnit(network, session)
                    userNumberFormat = userNumberFormat(asset.precision, withDecimalSeparator = false, withGrouping = true, locale = locale)
                    gdkNumberFormat = gdkNumberFormat(asset.precision)
                }
                isFiat -> {
                    asset = null
                    unitKey = getFiatCurrency(network, session)
                    userNumberFormat = userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = true, locale = locale)
                    gdkNumberFormat = gdkNumberFormat(decimals = 2, withDecimalSeparator = true)
                }
                else -> {
                    asset = null
                    unitKey = getUnit(network, session)
                    userNumberFormat = userNumberFormat(getDecimals(unitKey), withDecimalSeparator = false, withGrouping = true, locale = locale)
                    gdkNumberFormat = gdkNumberFormat(getDecimals(unitKey))
                }
            }

            return try{
                val input = if(i.isNullOrBlank()) "" else i
                val position = ParsePosition(0)

                val parsed = userNumberFormat.parse(input, position)

                if (position.index != input.length) {
                    throw Exception("id_error_parsing")
                }

                UserInput(gdkNumberFormat.format(parsed), parsed.toDouble(), assetId, gdkNumberFormat.minimumFractionDigits, unitKey , isFiat, asset = asset)
            }catch (e: Exception){
                if(throws){
                    throw e
                }else{
                    UserInput("", 0.0, assetId, gdkNumberFormat.minimumFractionDigits, unitKey , isFiat, asset = asset)
                }
            }
        }

        @Throws
        fun parseUserInput(session: GdkSession, input: String?, assetId: String? = null, isFiat: Boolean = false, locale: Locale = Locale.getDefault(), networkForTest: Network? = null): UserInput {
            return parse(session, input, assetId, isFiat, locale, throws = true, networkForTest = networkForTest)
        }

        fun parseUserInputSafe(session: GdkSession, input: String?, assetId: String? = null, isFiat: Boolean = false, locale: Locale = Locale.getDefault(), networkForTest: Network? = null): UserInput {
            return parse(session, input, assetId, isFiat, locale, throws = false, networkForTest = networkForTest)
        }
    }
}