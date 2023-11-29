package com.blockstream.common.utils

fun String?.hexToByteArray(): ByteArray {
    return if (this == null) {
        byteArrayOf()
    } else {
        ByteArray(this.length / 2) {
            this.substring(
                it * 2,
                it * 2 + 2
            ).toInt(16).toByte()
        }
    }
}

fun String?.hexToByteArrayReversed(): ByteArray {
    return hexToByteArray().also {
        it.reverse()
    }
}

internal val HEX = byteArrayOf(
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, // 0123456789
    65, 66, 67, 68, 69, 70 // ABCDEF
)

fun ByteArray.toHex(): String {
    val out = ByteArray(size * 2)
    var i = 0

    for (b in this) {
        val b1 = b.toInt()
        out[i++] = HEX[b1 and 0xf0 ushr 4]
        out[i++] = HEX[b1 and 0xf]
    }
    return out.decodeToString()
}

@ExperimentalUnsignedTypes
fun UByteArray.toHex(): String {
    val out = ByteArray(size * 2)
    var i = 0

    for (b in this) {
        val b1 = b.toInt()
        out[i++] = HEX[b1 and 0xf0 ushr 4]
        out[i++] = HEX[b1 and 0xf]
    }
    return out.decodeToString()
}