package com.blockstream.green.ui.drawer

import android.os.Bundle
import android.view.View
import com.blockstream.common.Urls
import com.blockstream.common.models.drawer.DrawerViewModel
import com.blockstream.common.models.wallets.WalletsViewModel
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.DrawerFragmentBinding
import com.blockstream.green.ui.wallet.AbstractWalletsFragment
import com.blockstream.green.utils.openBrowser
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class DrawerFragment : AbstractWalletsFragment<DrawerFragmentBinding>(R.layout.drawer_fragment, menuRes = 0) {
    override val screenName: String? = null

    override val isDrawer: Boolean = true

    private val viewModel: DrawerViewModel by viewModel()

    override fun getGreenViewModel() = viewModel

    override fun getAppViewModel() = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        init(binding.common, viewModel)

        binding.buttonSetupWallet.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalSetupNewWalletFragment())
            closeDrawer()
        }

        binding.buttonAppSettings.setOnClickListener {
            closeDrawer()
            navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
        }

        binding.helpCenter.setOnClickListener {
            closeDrawer()
            openBrowser(Urls.HELP_CENTER)
        }

        binding.about.setOnClickListener {
            closeDrawer()
            navigate(NavGraphDirections.actionGlobalAboutFragment())
        }
    }
}