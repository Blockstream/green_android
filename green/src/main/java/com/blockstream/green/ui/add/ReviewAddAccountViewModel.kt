package com.blockstream.green.ui.add

import com.blockstream.common.data.SetupArgs
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ReviewAddAccountViewModel constructor(
    val setupArgs: SetupArgs
) : AbstractAddAccountViewModel(setupArgs.greenWallet!!)