package com.blockstream.compose.looks.account

import com.blockstream.data.gdk.data.Address
import com.blockstream.data.gdk.data.Network

data class AddressLook(val address: String, val index: Long, val txCount: String, val canSign: Boolean) {

    companion object {
        fun create(address: Address, network: Network): AddressLook {
            return AddressLook(
                address = address.address,
                index = address.pointer,
                txCount = "${address.txCount ?: 0}",
                canSign = network.canSignMessage
            )
        }
    }
}