package com.blockstream.green.ui.wallet

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.common.models.wallets.WalletDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.ui.login.LoginFragment


abstract class AbstractWalletsFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

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