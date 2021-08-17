package com.blockstream.green.ui.looks

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.blockstream.green.R
import com.blockstream.green.gdk.GreenSession
import com.blockstream.gdk.data.Asset
import com.blockstream.green.gdk.getIcon
import com.blockstream.gdk.params.Convert
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.utils.asset
import com.blockstream.green.utils.btc
import com.blockstream.green.utils.getBitcoinOrLiquidUnit


class AssetListLook(
    private val id: String,
    val amount: Long,
    val asset: Asset?,
    val session: GreenSession
) {
    private var isLiquid: Boolean = session.network.isLiquid
    private var isMainnet: Boolean = session.network.isMainnet

    private val isBTCValue by lazy { id == session.network.policyAsset }

    val balance : String
        get() {
            return if(isBTCValue){
                session.convertAmount(Convert(amount)).btc(session, withUnit = false, withGrouping = true)
            }else{
                session.convertAmount(Convert(amount, asset)).asset(withUnit = false, withGrouping = true)
            }
        }

    val fiatValue by lazy {
        if (isBTCValue) {
            session.convertAmount(Convert(amount))
        } else {
            null
        }
    }

    val name: String
        get() {
            if (isBTCValue) {
                return if (isLiquid) {
                    "Liquid Bitcoin"
                } else {
                    "Bitcoin"
                }
            }
            return asset?.name ?: id
        }

    val ticker: String?
        get() {
            return if (id == session.network.policyAsset) {
                getBitcoinOrLiquidUnit(session)
            }else{
                asset?.ticker
            }
        }


    val issuer: String?
        get() {
            if (isBTCValue) {
                return "www.bitcoin.org"
            }

            return asset?.entity?.domain
        }


    fun icon(context: Context): Drawable {
        return when {
            isLiquid -> {
                id.getAssetIcon(context, session)
            }
            isMainnet -> {
                ContextCompat.getDrawable(context, R.drawable.ic_bitcoin_network_60)!!
            }
            else -> {
                ContextCompat.getDrawable(context, R.drawable.ic_bitcoin_testnet_network_60)!!
            }
        }
    }
}
