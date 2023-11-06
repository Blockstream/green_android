package com.blockstream.green.ui.drawer

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import com.blockstream.common.Urls
import com.blockstream.common.models.drawer.DrawerViewModel
import com.blockstream.common.models.wallets.WalletsViewModel
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.databinding.DrawerFragmentBinding
import com.blockstream.green.ui.wallet.AbstractWalletsFragment
import com.blockstream.green.utils.openBrowser
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class DrawerFragment : AbstractWalletsFragment<ComposeViewBinding>(R.layout.compose_view, menuRes = 0) {
    override val isDrawer: Boolean = true

    private val viewModel: DrawerViewModel by viewModel()

    override fun getGreenViewModel() = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(binding.composeView, viewModel)
    }
}