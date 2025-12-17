package com.blockstream.data.swap

enum class QuoteMode {
    SEND, RECEIVE;

    val isSend: Boolean
        get() = this == SEND
}