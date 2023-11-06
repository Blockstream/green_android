package com.blockstream.green.ui.wallet

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.databinding.ViewDataBinding
import com.blockstream.common.Urls
import com.blockstream.common.models.wallets.WalletDestinations
import com.blockstream.common.models.wallets.WalletsViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.screens.DrawerScreen
import com.blockstream.compose.screens.DrawerScreenCallbacks
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.HomeScreenCallbacks
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.ui.bottomsheets.DeleteWalletBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.RenameWalletBottomSheetDialogFragment
import com.blockstream.green.ui.login.LoginFragment
import com.blockstream.green.utils.openBrowser


abstract class AbstractWalletsFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    open val isDrawer = false

    fun init(composeView: ComposeView, viewModel: WalletsViewModel) {
        composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                GreenTheme {
                    if(isDrawer){
                        DrawerScreen(viewModel = viewModel, callbacks = DrawerScreenCallbacks(
                            onWalletClick = { wallet, isLightningShortcut ->
                                closeDrawer()
                                viewModel.postEvent(WalletsViewModel.LocalEvents.SelectWallet(wallet = wallet, isLightningShortcut = isLightningShortcut))
                            },
                            onNewWalletClick = {
                                closeDrawer()
                                navigate(NavGraphDirections.actionGlobalSetupNewWalletFragment())
                            },
                            onHelpClick = {
                                closeDrawer()
                                openBrowser(Urls.HELP_CENTER)
                            },
                            onAboutClick = {
                                closeDrawer()
                                navigate(NavGraphDirections.actionGlobalAboutFragment())
                            },
                            onAppSettingsClick = {
                                closeDrawer()
                                navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
                            }
                        ))
                    }else{
                        HomeScreen(viewModel = viewModel, callbacks = HomeScreenCallbacks(
                            onWalletClick = { wallet, isLightningShortcut ->
                                closeDrawer()
                                viewModel.postEvent(WalletsViewModel.LocalEvents.SelectWallet(wallet = wallet, isLightningShortcut = isLightningShortcut))
                            },
                            onWalletRename = {
                                RenameWalletBottomSheetDialogFragment.show(
                                    it,
                                    childFragmentManager
                                )
                            },
                            onWalletDelete = {
                                DeleteWalletBottomSheetDialogFragment.show(
                                    it,
                                    childFragmentManager
                                )
                            },
                            onNewWalletClick = {
                                closeDrawer()
                                navigate(NavGraphDirections.actionGlobalSetupNewWalletFragment())
                            },
                            onAboutClick = {
                                closeDrawer()
                                navigate(NavGraphDirections.actionGlobalAboutFragment())
                            },
                            onAppSettingsClick = {
                                closeDrawer()
                                navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
                            }
                        ))
                    }
                }
            }
        }

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