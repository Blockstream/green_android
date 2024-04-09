package com.blockstream.common.utils

import com.blockstream.common.data.Denomination
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Asset
import com.blockstream.common.gdk.data.Balance
import com.blockstream.common.gdk.params.Limits

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

    suspend fun getBalance(onlyInAcceptableRange: Boolean = true): Balance? {
        return if(amount.isNotBlank()){
            session.convert(assetId = assetId, asString = amount, denomination = denomination.denomination, onlyInAcceptableRange = onlyInAcceptableRange)
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

        @Throws(Exception::class)
        private fun parse(
            session: GdkSession,
            input: String,
            denomination: Denomination,
            assetId: String? = null,
            throws: Boolean = true,
            locale: String? = null
        ): UserInput {
            val unitKey : String
            // Users Locale
            val userNumberFormat : DecimalFormat
            // GDK format
            val gdkNumberFormat : DecimalFormat

            val asset: Asset?

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

            return try{
                val parsed = userNumberFormat.parseTo(input, gdkNumberFormat)

                UserInput(session = session, amount = parsed!!.first, amountAsDouble = parsed.second, denomination = denomination, assetId = assetId, asset = asset)
            }catch (e: Exception){
                if(throws){
                    throw e
                }else{
                    UserInput(session = session, amount = "", amountAsDouble = 0.0, denomination = denomination, assetId = assetId, asset = asset)
                }
            }
        }

        @Throws(Exception::class)
        fun parseUserInput(
            session: GdkSession,
            input: String?,
            denomination: Denomination? = null,
            assetId: String? = null,
            locale: String? = null
        ): UserInput {
            return parse(session = session, input = input?.trim() ?: "", denomination = denomination ?: Denomination.default(session), assetId = assetId, throws = true, locale = locale)
        }

        fun parseUserInputSafe(
            session: GdkSession,
            input: String?,
            denomination: Denomination? = null,
            assetId: String? = null,
            locale: String? = null
        ): UserInput {
            return parse(session = session, input = input?.trim() ?: "", denomination = denomination ?: Denomination.default(session), assetId = assetId, throws = false, locale = locale)
        }
    }
}