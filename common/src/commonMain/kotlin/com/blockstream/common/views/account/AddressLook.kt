package com.blockstream.common.views.account

import com.blockstream.common.gdk.data.Address
import com.blockstream.common.gdk.data.Network

data class AddressLook(val address: Address, val txCount: String, val canSign: Boolean) {

    companion object {
        fun create(address: Address, network: Network): AddressLook {
            return AddressLook(
                address = address,
                txCount = "${address.txCount ?: 0}",
                canSign = network.canSignMessage
            )
        }
    }
}