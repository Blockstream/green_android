package com.blockstream.green.ui.bottomsheets


import androidx.databinding.ViewDataBinding
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.Network
import com.blockstream.green.database.Wallet
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel


abstract class WalletBottomSheetDialogFragment<T : ViewDataBinding, VM : AbstractWalletViewModel> : AbstractBottomSheetDialogFragment<T>() {
    override val segmentation: HashMap<String, Any>?
        get() = countly.accountSegmentation(
            session = session,
            account = accountOrNull
        )

    @Suppress("UNCHECKED_CAST")
    internal val viewModel : VM by lazy {
        (requireParentFragment() as AbstractWalletFragment<*>).getWalletViewModel() as VM
    }

    val session
        get() = viewModel.session

    val wallet: Wallet
        get() = viewModel.wallet

    open val accountOrNull: Account?
        get() = viewModel.let { viewModel -> if(viewModel is AbstractAccountWalletViewModel) viewModel.account else null }

    open val account: Account
        get() = accountOrNull!!

    open val network: Network by lazy {
        account.network
    }
}