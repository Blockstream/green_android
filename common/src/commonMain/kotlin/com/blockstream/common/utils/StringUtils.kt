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