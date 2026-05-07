package com.blockstream.data.extensions

// Helper fn for Data Binding as the original fn is InlineOnly
fun String?.isBlank() = isNullOrBlank()
fun String?.isNotBlank() = !isNullOrBlank()

fun String?.padHex() = this?.replace("........".toRegex(), "$0 ")

fun String?.cleanup(): String? = if (isNullOrBlank()) null else trim().replace("\n", "")

fun String.isDigitsOnly() = all(Char::isDigit) && isNotEmpty()

fun List<String>?.startsWith(other: String?): Boolean = if (this == null) false else
    other.takeIf { it.isNotBlank() }?.let { o -> any { o.startsWith(it) } } ?: false

fun String?.isHostPortFormatValid(): Boolean {
    val text = this ?: return true
    if (text.isBlank()) return true

    val lastColonIndex = text.lastIndexOf(':')

    if (lastColonIndex == -1 || lastColonIndex == text.length - 1) return false

    val host = text.substring(0, lastColonIndex)
    val portString = text.substring(lastColonIndex + 1)

    return host.isNotBlank() && portString.isValidPort()
}

fun String.isValidPort(): Boolean {
    val port = this.toIntOrNull()
    return port != null && port in 1..UShort.MAX_VALUE.toInt() // 65535
}