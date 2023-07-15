package com.blockstream.green.ui.wallet

import com.blockstream.common.data.GreenWallet
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

/*
 * This is an implementation of AbstractWalletViewModel so that can easily be used by fragments without
 * needing to implement their own VM. Add any required methods to the Abstracted class.
 */
@KoinViewModel
class WalletViewModel constructor(
    @InjectedParam
    wallet: GreenWallet,
) : AbstractWalletViewModel(wallet)