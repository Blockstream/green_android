package com.blockstream.green.data

import android.os.Parcelable
import com.blockstream.common.BITS_UNIT
import com.blockstream.common.BTC_UNIT
import com.blockstream.common.MBTC_UNIT
import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.UBTC_UNIT
import com.blockstream.common.gdk.data.Balance
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.assetTicker
import com.blockstream.green.utils.getBitcoinOrLiquidUnit
import com.blockstream.green.utils.toAmountLook
import kotlinx.parcelize.Parcelize

import mu.KLogging

@Parcelize
data class DenominatedValue constructor(
    val balance: Balance?,
    val assetId: String?,
    val denomination: Denomination
) : Parcelable {


    fun asNetworkUnit(session: GdkSession): String {
        return if (denomination.isFiat) {
            denomination.denomination
        } else {
            getBitcoinOrLiquidUnit(
                session = session,
                assetId = assetId,
                denomination = denomination
            )
        }
    }

    fun asLook(session: GdkSession): String? {
        return balance?.toAmountLook(
            session = session,
            assetId = assetId,
            withUnit = true,
            withGrouping = true,
            withMinimumDigits = false,
            denomination = denomination
        )
    }

    fun asInput(session: GdkSession): String? {
        return balance?.toAmountLook(
            session = session,
            assetId = assetId,
            withUnit = false,
            withGrouping = false,
            withMinimumDigits = false,
            denomination = denomination
        )
    }

    companion object : KLogging() {
        fun fromBalance(balance: Balance?, assetId: String?, denomination: Denomination): DenominatedValue {
            return DenominatedValue(balance = balance, assetId = assetId, denomination = denomination)
        }

        fun toDenomination(denominatedValue: DenominatedValue, denomination: Denomination): DenominatedValue {
            return DenominatedValue(balance = denominatedValue.balance, assetId = denominatedValue.assetId, denomination = denomination)
        }

        fun createDefault(session: GdkSession): DenominatedValue {
            return DenominatedValue(
                balance = null,
                assetId = null,
                denomination = Denomination.byUnit((session.getSettings()?.unit ?: BTC_UNIT))
            )
        }
    }
}

@Parcelize
sealed class Denomination(open val denomination: String) : Parcelable {
    object BTC : Denomination(BTC_UNIT)
    object MBTC : Denomination(MBTC_UNIT)
    object UBTC : Denomination(UBTC_UNIT)
    object BITS : Denomination(BITS_UNIT)
    object SATOSHI : Denomination(SATOSHI_UNIT)

    class FIAT(override val denomination: String) : Denomination(denomination)

    override fun toString(): String {
        return denomination
    }

    fun unit(session: GdkSession, assetId: String?): String = if (this is FIAT){
        this.denomination
    }else {
        getBitcoinOrLiquidUnit(session = session, assetId = assetId, denomination = this)
    }

    fun assetTicker(session: GdkSession, assetId: String?): String = if (this is FIAT){
        denomination
    }else {
        assetId.assetTicker(session = session, denomination = this)
    }

    val isFiat
        get() = this is FIAT

    fun notFiat(): Denomination?{
        return this.takeIf { !it.isFiat }
    }

    companion object : KLogging() {

        fun byUnit(unit: String) = when (unit) {
            BTC_UNIT -> BTC
            MBTC_UNIT -> MBTC
            UBTC_UNIT -> UBTC
            BITS_UNIT -> BITS
            SATOSHI_UNIT -> SATOSHI
            else -> FIAT(unit)
        }

        fun fiat(session: GdkSession): Denomination?{
            return session.getSettings()?.pricing?.exchange?.let { FIAT(it) }
        }

        fun fiatOrNull(session: GdkSession, isFiat: Boolean): Denomination?{
            return if(isFiat) fiat(session) else null
        }

        fun exchange(session: GdkSession, denomination: Denomination?): Denomination?{
            return if(denomination?.isFiat == true) default(session) else fiat(session)
        }

        fun default(session: GdkSession): Denomination {
            return byUnit((session.getSettings()?.unit ?: BTC_UNIT))
        }

        fun defaultOrFiat(session: GdkSession, isFiat: Boolean): Denomination{
            return (if(isFiat) fiat(session) else default(session)) ?: default(session)
        }
    }
}