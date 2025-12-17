package com.blockstream.data.swap

enum class SwapAsset(val symbol: String, val isLightning: Boolean) {
    Bitcoin("BTC", false),
    Liquid("L-BTC", false),
    Lightning("BTC", true);

    val isChain = !isLightning
}