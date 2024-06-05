package com.blockstream.compose.utils

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun ByteArray?.toPainter(): Painter? {
    if(this != null) {
        return BitmapPainter(Image.makeFromEncoded(this).toComposeImageBitmap())
    }
    return null
}