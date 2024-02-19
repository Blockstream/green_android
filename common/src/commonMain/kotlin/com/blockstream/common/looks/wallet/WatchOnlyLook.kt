package com.blockstream.common.looks.wallet

import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Network

data class WatchOnlyLook constructor(
    val account: Account? = null,
    val network: Network? = null,
    val username: String? = null,
    val outputDescriptors: String? = null,
    val extendedPubkey: String? = null
)