package com.blockstream.green.ui.add

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import androidx.navigation.fragment.findNavController
import com.blockstream.common.events.Event
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.add.AddAccountViewModelAbstract
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.extensions.setNavigationResult
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.dialogs.EnableLightningShortcut
import com.blockstream.green.ui.dialogs.LightningShortcutDialogFragment

abstract class AbstractAddAccountFragment<T : ViewDataBinding>(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
): AppFragment<T>(layout, menuRes), EnableLightningShortcut {
    open val network: Network? = null

    override val title: String?
        get() = network?.let { (if(it.isBitcoin) getGreenViewModel().session.bitcoin else getGreenViewModel().session.liquid)?.canonicalName }

    override val toolbarIcon: Int?
        get() = network?.getNetworkIcon()

    abstract val assetId: String?

    private var _pendingEvent: Event? = null

    abstract val viewModel: AddAccountViewModelAbstract

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if(sideEffect is SideEffects.Navigate){
            (sideEffect.data as? Account)?.also {
                navigate(it)
            }
        } else if(sideEffect is AddAccountViewModelAbstract.LocalSideEffects.LightningShortcutDialog){
            _pendingEvent = sideEffect.event
            LightningShortcutDialogFragment.show(fragmentManager = childFragmentManager)
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

    override fun lightningShortcutDialogDismissed() {
        _pendingEvent?.also {
            viewModel.postEvent(it)
        }
    }

    companion object {
        const val SET_ACCOUNT = "SET_ACCOUNT"
    }
}