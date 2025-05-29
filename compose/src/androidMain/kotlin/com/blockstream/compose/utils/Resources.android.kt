package com.blockstream.compose.utils

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter

actual fun ByteArray?.toPainter(): Painter? {
    if (this != null) {
        try {
            return BitmapPainter(BitmapFactory.decodeByteArray(this, 0, this.size).asImageBitmap())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}