package com.blockstream.common.extensions

// Helper fn for Data Binding as the original fn is InlineOnly
fun String?.isBlank() = isNullOrBlank()
fun String?.isNotBlank() = !isNullOrBlank()

fun String?.padHex() = this?.replace("........".toRegex(), "$0 ")

fun String?.cleanup(): String? = if (isNullOrBlank()) null else trim().replace("\n", "")

fun List<String>?.startsWith(other: String?): Boolean = if(this == null) false else
    other.takeIf { it.isNotBlank() }?.let { o -> any { o.startsWith(it) } } ?: false