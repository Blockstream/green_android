package com.blockstream.common.looks.account

import com.blockstream.common.gdk.data.Account
import com.blockstream.common.utils.Loggable

data class AccountLook(val account: Account, val balance: Long) {
    val name
        get() = account.name

    companion object: Loggable() {
        fun create(account: Account): AccountLook {
            return AccountLook(account, 0)
        }
    }
}