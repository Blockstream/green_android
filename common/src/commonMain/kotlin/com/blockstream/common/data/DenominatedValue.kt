package com.blockstream.common.data

import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import com.blockstream.common.BITS_UNIT
import com.blockstream.common.BTC_UNIT
import com.blockstream.common.MBTC_UNIT
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.UBTC_UNIT
import com.blockstream.common.extensions.assetTicker
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.Balance
import com.blockstream.common.utils.getBitcoinOrLiquidUnit
import com.blockstream.common.utils.toAmountLook
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
data class DenominatedValue constructor(
    val denomination: Denomination,
    val balance: Balance? = null,
    val assetId: String? = null,
    val asInput: String? = null,
    val asLook: String? = null
): GreenJson<DenominatedValue>(), Parcelable, JavaSerializable {
    override fun kSerializer() = serializer()

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

    companion object {
        fun fromBalance(balance: Balance?, assetId: String?, denomination: Denomination): DenominatedValue {
            return DenominatedValue(balance = balance, assetId = assetId, denomination = denomination)
        }

        fun toDenominationDeprecated(denominatedValue: DenominatedValue, denomination: Denomination): DenominatedValue {
            return DenominatedValue(balance = denominatedValue.balance, assetId = denominatedValue.assetId, denomination = denomination)
        }

        suspend fun toDenomination(denominatedValue: DenominatedValue, denomination: Denomination, session: GdkSession): DenominatedValue {
            return DenominatedValue(
                denomination = denomination,
                balance = denominatedValue.balance,
                assetId = denominatedValue.assetId,
                asInput = denominatedValue.balance?.toAmountLook(
                    session = session,
                    assetId = denominatedValue.assetId,
                    withUnit = false,
                    withGrouping = false,
                    withMinimumDigits = false,
                    denomination = denomination
                ),
                asLook = denominatedValue.balance?.toAmountLook(
                    session = session,
                    assetId = denominatedValue.assetId,
                    withUnit = true,
                    withGrouping = true,
                    withMinimumDigits = false,
                    denomination = denomination
                )
            )
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

@Serializable
@Parcelize
sealed class Denomination : Parcelable, JavaSerializable {
    abstract val denomination: String
    @Serializable
    object BTC: Denomination() {
        override val denomination: String = BTC_UNIT
    }
    @Serializable
    object MBTC : Denomination(){
        override val denomination: String = MBTC_UNIT
    }

    @Serializable
    object UBTC : Denomination(){
        override val denomination: String = UBTC_UNIT
    }

    @Serializable
    object BITS : Denomination(){
        override val denomination: String = BITS_UNIT
    }

    @Serializable
    object SATOSHI : Denomination(){
        override val denomination: String = SATOSHI_UNIT
    }

    @Serializable
    class FIAT(override val denomination: String) : Denomination()

    override fun toString(): String {
        return denomination
    }

    fun unit(session: GdkSession, assetId: String? = null): String = if (this is FIAT){
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

    companion object {

        fun byUnit(unit: String) = when (unit) {
            BTC_UNIT -> BTC
            MBTC_UNIT -> MBTC
            UBTC_UNIT -> UBTC
            BITS_UNIT -> BITS
            SATOSHI_UNIT -> SATOSHI
            else -> FIAT(unit)
        }

        fun fiat(session: GdkSession): Denomination?{
            return session.getSettings()?.pricing?.currency?.let { FIAT(it) }
        }

        fun fiatOrNull(session: GdkSession, isFiat: Boolean): Denomination?{
            return if(isFiat) fiat(session) else null
        }

        fun exchange(session: GdkSession, denomination: Denomination?): Denomination?{
            return if(denomination?.isFiat == true) default(session) else fiat(session)
        }

        fun default(session: GdkSession): Denomination {
            return session.ifConnected { byUnit((session.getSettings()?.unit ?: BTC_UNIT)) } ?: BTC
        }

        fun defaultOrFiat(session: GdkSession, isFiat: Boolean): Denomination {
            return (if(isFiat) fiat(session) else default(session)) ?: default(session)
        }
    }
}