package com.blockstream.domain.send

object SendActionAvailability {
    // Balance is not a precondition, the send flow reports insufficient funds itself.
    fun isEnabled(isMultisigWatchOnly: Boolean): Boolean = !isMultisigWatchOnly
}
