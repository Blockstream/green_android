package com.blockstream.green.utils

import com.blockstream.gdk.params.Convert
import com.blockstream.green.gdk.GreenSession
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.jvm.Throws

// Parse user input respecting user Locale and convert the value to GDK compatible value (US Locale)
data class UserInput(val amount: String, val decimals: Int, val unitKey: String, val isFiat: Boolean) {

    fun toLimit(): JsonElement {
        return buildJsonObject {
            put("is_fiat", isFiat)
            put(if(isFiat) "fiat" else unitKey, amount)
        }
    }

    fun getBalance(session: GreenSession) = session.convertAmount(Convert.forUnit(unitKey, amount))

    companion object{
        @Throws
        fun parseUserInput(session: GreenSession, input: String?, isFiat: Boolean = false, locale: Locale = Locale.getDefault()): UserInput {
            val unitKey : String
            // Users Locale
            val userNumberFormat : DecimalFormat
            // GDK format
            val gdkNumberFormat : DecimalFormat

            if(isFiat){
                unitKey = getFiatCurrency(session)
                userNumberFormat = userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = true, locale = locale)
                gdkNumberFormat = gdkNumberFormat(decimals = 2, withDecimalSeparator = true)
            }else{
                unitKey = getUnit(session)
                userNumberFormat = userNumberFormat(getDecimals(unitKey), withDecimalSeparator = false, withGrouping = true, locale = locale)
                gdkNumberFormat = gdkNumberFormat(getDecimals(unitKey))
            }

            val parsed = userNumberFormat.parse(if(input.isNullOrBlank()) "0" else input)
            return UserInput(gdkNumberFormat.format(parsed), gdkNumberFormat.minimumFractionDigits, unitKey , isFiat)
        }
    }
}