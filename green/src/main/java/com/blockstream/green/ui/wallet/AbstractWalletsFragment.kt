package com.blockstream.green.ui.wallet

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.common.navigation.NavigateDestination
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.ui.AppFragment

abstract class AbstractWalletsFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        (sideEffect as? SideEffects.NavigateTo)?.also {
            navigate(it.destination)
        }
    }

    internal fun navigate(directions: NavigateDestination) {
        when (directions) {
            is NavigateDestinations.SetupNewWallet -> {
                closeDrawer()
                navigate(NavGraphDirections.actionGlobalSetupNewWalletFragment())
            }

            is NavigateDestinations.About -> {
                closeDrawer()
            }

            is NavigateDestinations.WalletOverview -> navigate(
                NavGraphDirections.actionGlobalWalletOverviewFragment(
                    directions.greenWallet
                )
            )
        }
    }
}