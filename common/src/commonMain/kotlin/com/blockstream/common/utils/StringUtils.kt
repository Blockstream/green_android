package com.blockstream.common.utils

// https://stackoverflow.com/questions/9607903/get-domain-name-from-given-url/9608008#9608008
fun String.hostname(excludePort: Boolean = false): String {
    var start = indexOf("://")
    if (start < 0) {
        start = 0
    } else {
        start += 3
    }
    var end = indexOf("/", start)
    if (end < 0) {
        end = length
    }
    var hostname = substring(start, end)

    if(excludePort) {
        val port = hostname.indexOf(":")
        if (port >= 0) {
            hostname = hostname.substring(0, port)
        }
    }

    return hostname
}

fun String.server(): String {
    var start = indexOf("://")
    if (start < 0) {
        start = 0
    } else {
        start += 3
    }
    var end = indexOf("/", start)
    if (end < 0) {
        end = length
    }

    return substring(0, end)
}

private val EmailAddressRegex = Regex(
    "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
)

fun String.isEmailValid() = matches(EmailAddressRegex)

fun String.nthIndexOf(substring: String, nth: Int): Int {
    return if (nth == 1) {
        indexOf(substring)
    } else {
        indexOf(substring, nthIndexOf(substring, nth - 1) + substring.length)
    }
}

fun String.lastNthIndexOf(substring: String, nth: Int): Int {
    return if (nth == 1) {
        lastIndexOf(substring)
    } else {
        lastIndexOf(substring, lastNthIndexOf(substring, nth - 1) - substring.length)
    }
}