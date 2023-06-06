package com.blockstream.gdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import com.blockstream.common.gdk.data.Pricing
import com.blockstream.common.gdk.data.Transaction
import java.util.Date

fun ByteArray.reverseBytes(): ByteArray {
    for (i in 0 until size / 2) {
        val b = this[i]
        this[i] = this[size - i - 1]
        this[size - i - 1] = b
    }
    return this
}

fun ByteArray?.toBitmap(): Bitmap?{
    if(this != null) {
        try {
            return BitmapFactory.decodeByteArray(this, 0, this.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}

fun Bitmap.toBitmapDrawable(context: Context): BitmapDrawable{
    return BitmapDrawable(context.resources, this)
}

fun Pricing.toString(context: Context, res: Int): String {
    return context.getString(res, currency, exchange)
}

fun Transaction.createdAt(): Date {
    return Date(createdAtTs / 1000)
}