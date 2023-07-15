package com.blockstream.green.ui.wallet

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.models.wallets.WalletDestinations
import com.blockstream.common.models.wallets.WalletsViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.views.wallet.WalletListLook
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.AbstractWalletsFragmentBinding
import com.blockstream.green.databinding.ListItemWalletBinding
import com.blockstream.green.extensions.showPopupMenu
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.ui.bottomsheets.DeleteWalletBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.RenameWalletBottomSheetDialogFragment
import com.blockstream.green.ui.items.WalletListItem
import com.blockstream.green.ui.login.LoginFragment
import com.blockstream.green.utils.observeFlow
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


abstract class AbstractWalletsFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    open val isDrawer = false

    fun init(
        binding: AbstractWalletsFragmentBinding,
        viewModel: WalletsViewModel
    ) {
        binding.vm = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val softwareWalletsModel = ModelAdapter { view: WalletListLook ->
            WalletListItem(look = view)
        }.observeFlow(lifecycleScope, viewModel.softwareWallets, useDiffUtil = false, toList = {
            it ?: emptyList()
        })

        val softwareWalletsAdapter = FastAdapter.with(softwareWalletsModel)

        softwareWalletsAdapter.onClickListener = { _, _, item, _ ->
            viewModel.postEvent(WalletsViewModel.LocalEvents.SelectWallet(wallet = item.look.greenWallet))
            closeDrawer()
            true
        }

        softwareWalletsAdapter.addClickListener<ListItemWalletBinding, WalletListItem>({ binding -> binding.lightningShortcut }) { _, _, _, item ->
            viewModel.postEvent(WalletsViewModel.LocalEvents.SelectWallet(wallet = item.look.greenWallet, isLightningShortcut = true))
            closeDrawer()
        }

        val ephemeralWalletsModel = ModelAdapter { view: WalletListLook ->
            WalletListItem(look = view)
        }.observeFlow(lifecycleScope, viewModel.ephemeralWallets, useDiffUtil = false, toList = {
            it ?: emptyList()
        })


        val ephemeralWalletsAdapter = FastAdapter.with(ephemeralWalletsModel)

        ephemeralWalletsAdapter.onClickListener = { _, _, item, _ ->
            viewModel.postEvent(WalletsViewModel.LocalEvents.SelectWallet(wallet = item.look.greenWallet))
            closeDrawer()
            true
        }

        val hardwareWalletsModel = ModelAdapter { view: WalletListLook ->
            WalletListItem(look = view)
        }.observeFlow(lifecycleScope, viewModel.hardwareWallets, useDiffUtil = false, toList = {
            it ?: emptyList()
        })

        val hardwareWalletsAdapter = FastAdapter.with(hardwareWalletsModel)

        hardwareWalletsAdapter.onClickListener = { _, _, item, _ ->
            viewModel.postEvent(WalletsViewModel.LocalEvents.SelectWallet(wallet = item.look.greenWallet))
            closeDrawer()
            true
        }

        if (!isDrawer) {
            listOf(softwareWalletsAdapter, hardwareWalletsAdapter).forEach {
                it.onLongClickListener = { view, _, item, _ ->
                    showPopupMenu(view, R.menu.menu_wallet) { menuItem ->
                        when (menuItem.itemId) {
                            R.id.delete -> {
                                DeleteWalletBottomSheetDialogFragment.show(
                                    item.look.greenWallet,
                                    childFragmentManager
                                )
                            }

                            R.id.rename -> {
                                RenameWalletBottomSheetDialogFragment.show(
                                    item.look.greenWallet,
                                    childFragmentManager
                                )
                            }
                        }
                        true
                    }

                    true
                }
            }
        }

        binding.recyclerSoftwareWallets.apply {
            adapter = softwareWalletsAdapter
            itemAnimator = SlideDownAlphaAnimator()
        }

        binding.recyclerEphemeralWallets.apply {
            adapter = ephemeralWalletsAdapter
            itemAnimator = SlideDownAlphaAnimator()
        }

        binding.recyclerHardwareWallets.apply {
            adapter = hardwareWalletsAdapter
            itemAnimator = SlideDownAlphaAnimator()
        }

        sessionManager.connectionChangeEvent.onEach {
            softwareWalletsAdapter.notifyAdapterDataSetChanged()
            ephemeralWalletsAdapter.notifyAdapterDataSetChanged()
            hardwareWalletsAdapter.notifyAdapterDataSetChanged()
        }.launchIn(lifecycleScope)
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        ((sideEffect as? SideEffects.NavigateTo)?.destination as? WalletDestinations)?.also {
            navigate(it)
        }
    }

    internal fun navigate(directions: WalletDestinations) {

        when (directions) {
            is WalletDestinations.WalletOverview -> navigate(
                NavGraphDirections.actionGlobalWalletOverviewFragment(
                    directions.wallet
                )
            )

            is WalletDestinations.WalletLogin -> {

                (requireActivity() as MainActivity).getVisibleFragment()?.also {
                    if(it is LoginFragment && it.viewModel.greenWalletOrNull == directions.wallet && it.args.isLightningShortcut == directions.isLightningShortcut){
                        return
                    }
                }

                navigate(
                    NavGraphDirections.actionGlobalLoginFragment(
                        wallet = directions.wallet,
                        isLightningShortcut = directions.isLightningShortcut,
                        autoLoginWallet = !directions.isLightningShortcut
                    )
                )
            }

            is WalletDestinations.DeviceScan -> navigate(
                NavGraphDirections.actionGlobalDeviceScanFragment(
                    wallet = directions.wallet
                )
            )
        }
    }
}