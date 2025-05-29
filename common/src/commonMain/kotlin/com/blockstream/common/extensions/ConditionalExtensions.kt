package com.blockstream.common.extensions

inline fun Boolean.ifTrue(block: (Boolean) -> Unit): Boolean {
    if (this) {
        block(this)
    }
    return this
}

inline fun Boolean.ifFalse(block: (Boolean) -> Unit): Boolean {
    if (!this) {
        block(this)
    }
    return this
}