package com.blockstream.green.ui.wallet

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.Network
import mu.KLogging

abstract class AbstractAccountWalletFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AbstractWalletFragment<T>(layout, menuRes) {

    abstract fun getAccountWalletViewModel(): AbstractAccountWalletViewModel

    override fun getWalletViewModel() = getAccountWalletViewModel()

    val account: Account
        get() = getAccountWalletViewModel().account

    val network: Network
        get() = account.network

    // Prevent ViewModel initialization if session is not initialized
    override val subtitle: String?
        get() = if (isSessionNetworkInitialized) if(overrideSubtitle) wallet.name else super.subtitle else null

    open val overrideSubtitle = true

    companion object : KLogging()
}