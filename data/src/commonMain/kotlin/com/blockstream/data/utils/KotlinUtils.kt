package com.blockstream.data.utils

inline fun <A, B, R> ifNotNull(a: A?, b: B?, block: (A, B) -> R): R? {
    return if (a != null && b != null) {
        block(a, b)
    } else {
        null
    }
}