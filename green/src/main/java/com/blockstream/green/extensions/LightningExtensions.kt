package com.blockstream.green.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import breez_sdk.LnInvoice
import com.blockstream.common.lightning.expireIn
import com.blockstream.common.lightning.lnUrlPayImage
import java.util.Date

fun LnInvoice.expireInAsDate() = Date(expireIn().toEpochMilliseconds())

fun List<List<String>>?.lnUrlPayBitmap(): Bitmap? {
    return lnUrlPayImage()?.let {
        try{
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }
}