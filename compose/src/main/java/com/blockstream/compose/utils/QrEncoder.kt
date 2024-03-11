package com.blockstream.compose.utils

import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import com.lightspark.composeqr.QrEncoder

class QrEncoder(private val errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.L) : QrEncoder {
    override fun encode(qrData: String): ByteMatrix? {
        return Encoder.encode(
            qrData,
            errorCorrection,
            mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 16,
                EncodeHintType.ERROR_CORRECTION to errorCorrection,
            )
        ).matrix
    }
}