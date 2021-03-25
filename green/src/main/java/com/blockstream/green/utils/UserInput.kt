package com.blockstream.green.utils

import com.blockstream.green.gdk.GreenSession
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.util.*
import kotlin.jvm.Throws

data class UserInput(val amount: String, val decimals: Int, val unitKey: String, val isFiat: Boolean) {

    fun toLimit(): JsonElement {
        return buildJsonObject {
            put("is_fiat", isFiat)
            put(if(isFiat) "fiat" else unitKey, amount)
        }
    }

    companion object{
        private fun getNumberFormat(decimals: Int, locale: Locale = Locale.getDefault()) = NumberFormat.getInstance(locale).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }

        @Throws
        fun parseUserInput(session: GreenSession, input: String?, isFiat: Boolean = false, locale: Locale = Locale.getDefault()): UserInput {
            val unitKey : String
            val numberFormat : NumberFormat

            if(isFiat){
                unitKey = session.getSettings()!!.pricing.currency
                numberFormat = getNumberFormat(2, locale)
            }else{
                unitKey = session.getSettings()!!.unitKey
                numberFormat = getNumberFormat(getDecimals(unitKey), locale)
            }

            val parsed = numberFormat.parse(if(input.isNullOrBlank()) "0" else input)!!.toDouble().toString()
            return UserInput(parsed, numberFormat.minimumFractionDigits, unitKey , isFiat)
        }
    }
}