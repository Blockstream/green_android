package com.blockstream.green.ui.drawer

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockstream.common.models.drawer.DrawerViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.DrawerScreen
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.wallet.AbstractWalletsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel


class DrawerFragment :
    AbstractWalletsFragment<ComposeViewBinding>(R.layout.compose_view, menuRes = 0) {

    private val viewModel: DrawerViewModel by viewModel()

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        closeDrawer()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    DrawerScreen(viewModel = viewModel)
                }
            }
        }
    }
}