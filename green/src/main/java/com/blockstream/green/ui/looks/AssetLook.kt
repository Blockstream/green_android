package com.blockstream.green.ui.looks

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.blockstream.green.R
import com.blockstream.green.gdk.GreenSession
import com.blockstream.gdk.data.Asset
import com.blockstream.gdk.data.Balance
import com.blockstream.gdk.params.Convert
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.utils.*


class AssetLook constructor(
    private val id: String,
    val amount: Long,
    val session: GreenSession
) {
    private val asset = session.getAsset(id)
    private var isLiquid: Boolean = session.network.isLiquid
    private var isMainnet: Boolean = session.network.isMainnet

    private val isBTCValue by lazy { id == session.network.policyAsset }

    fun balance(withUnit: Boolean = false) : String {
            return if(isBTCValue){
                amount.toBTCLook(session, withUnit = withUnit, withGrouping = true)
            }else{
                amount.toAssetLook(session, assetId = id, withUnit = withUnit, withGrouping = true)
            }
        }

    val fiatValue : Balance?
        get() = if (isBTCValue) {
                session.convertAmount(Convert(satoshi = amount))
            } else {
                null
            }

    val name: String
        get() {
            if (isBTCValue) {
                return if (isLiquid) {
                    "Liquid Bitcoin"
                } else {
                    "Bitcoin"
                }.let {
                    if(session.network.isTestnet) "Testnet $it" else it
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


    fun icon(context: Context): Drawable = id.getAssetIcon(context, session)
}
