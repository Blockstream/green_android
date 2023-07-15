package com.blockstream.green.ui.add

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import androidx.navigation.fragment.findNavController
import com.blockstream.common.Urls
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.setNavigationResult
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.dialogs.EnableLightningShortcut
import com.blockstream.green.ui.dialogs.LightningShortcutDialogFragment
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class AbstractAddAccountFragment<T : ViewDataBinding>(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
): AbstractWalletFragment<T>(layout, menuRes), EnableLightningShortcut {
    open val network: Network? = null

    override val title: String?
        get() = network?.let { (if(it.isBitcoin) session.bitcoin else session.liquid)?.canonicalName }

    override val toolbarIcon: Int?
        get() = network?.getNetworkIcon()

    abstract val addAccountViewModel: AbstractAddAccountViewModel

    abstract val assetId: String?

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        addAccountViewModel.accountCreated.observe(viewLifecycleOwner) { account ->
            if (account.isLightning && !wallet.isEphemeral) {
                LightningShortcutDialogFragment.show(isAddAccount = true, fragmentManager = childFragmentManager)
            } else {
                navigate(account)
            }
        }

        addAccountViewModel.onError.observe(viewLifecycleOwner) {
            it.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }
    }

    private fun navigate(account: Account){
        // Find if there is a Receive screen in the backstack or a Network overview
        val destinationId = findNavController().currentBackStack.value.let { backQueue ->
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

    override fun getWalletViewModel(): AbstractWalletViewModel = addAccountViewModel

    override fun enableLightningShortcut(){
        addAccountViewModel.enableLightningShortcut()
    }

    override fun lightningShortcutDialogDismissed() {
        addAccountViewModel.accountCreated.value?.also {
            navigate(it)
        }
    }

    companion object {
        const val SET_ACCOUNT = "SET_ACCOUNT"
    }
}