package com.blockstream

import com.blockstream.gdk.data.Network
import com.greenaddress.greenapi.HWWalletBridge

interface HwWalletLogin: HWWalletBridge {
    fun requestNetwork(): Network?
}