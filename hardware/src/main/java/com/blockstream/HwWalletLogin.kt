package com.blockstream

import com.blockstream.common.gdk.device.HardwareWalletInteraction
import com.blockstream.common.gdk.data.Network

interface HwWalletLogin: HardwareWalletInteraction {
    fun requestNetwork(): Network?
}