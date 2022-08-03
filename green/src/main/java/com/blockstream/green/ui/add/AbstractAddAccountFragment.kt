package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import androidx.navigation.fragment.findNavController
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.setNavigationResult
import javax.inject.Inject

abstract class AbstractAddAccountFragment<T : ViewDataBinding>(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
): AbstractWalletFragment<T>(layout, menuRes) {
    @Inject
    lateinit var gdkBridge: GdkBridge

    open val network: Network? = null

    override val title: String?
        get() = network?.let { (if(it.isBitcoin) session.bitcoin else session.liquid)?.canonicalName }

    override val toolbarIcon: Int?
        get() = network?.getNetworkIcon()

    abstract val addAccountViewModel: AbstractAddAccountViewModel

    abstract val assetId: String?

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        addAccountViewModel.accountCreated.observe(viewLifecycleOwner) { account ->
            // Find if there is a Receive screen in the backstack or a Network overview
            val destinationId = findNavController().backQueue.let { backQueue ->
                (backQueue.find { it.destination.id == R.id.receiveFragment } ?: backQueue.find { it.destination.id == R.id.walletOverviewFragment })!!.destination.id
            }

            setNavigationResult(
                result = AccountAsset(
                    account = account,
                    assetId = (assetId ?: account.network.policyAsset)
                ), key = SET_ACCOUNT, destinationId = destinationId
            )
            findNavController().popBackStack(destinationId, false)
        }

        addAccountViewModel.onError.observe(viewLifecycleOwner) {
            it.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = addAccountViewModel

    companion object {
        const val SET_ACCOUNT = "SET_ACCOUNT"
    }
}