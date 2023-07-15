package com.blockstream.green.ui.bottomsheets


import androidx.databinding.ViewDataBinding
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.ui.wallet.AbstractWalletFragment


abstract class WalletBottomSheetDialogFragment<T : ViewDataBinding, VM : GreenViewModel> : AbstractBottomSheetDialogFragment<T>() {
    override val segmentation: HashMap<String, Any>?
        get() = countly.accountSegmentation(
            session = session,
            account = accountOrNull
        )

    @Suppress("UNCHECKED_CAST")
    internal val viewModel : VM by lazy {
        (requireParentFragment() as? AbstractWalletFragment<*>)?.getWalletViewModel() as? VM
            ?: (requireParentFragment() as AppFragment<*>).getGreenViewModel() as VM
    }

    val session
        get() = viewModel.session

    val wallet: GreenWallet
        get() = viewModel.greenWallet

    open val accountOrNull: Account?
        get() = viewModel.let { viewModel -> if(viewModel is AbstractAccountWalletViewModel) viewModel.accountValue else null }

    open val account: Account
        get() = accountOrNull!!

    open val network: Network by lazy {
        account.network
    }
}