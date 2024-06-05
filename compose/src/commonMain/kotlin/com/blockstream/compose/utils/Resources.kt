package com.blockstream.compose.utils

import androidx.compose.ui.graphics.painter.Painter

expect fun ByteArray?.toPainter(): Painter?
