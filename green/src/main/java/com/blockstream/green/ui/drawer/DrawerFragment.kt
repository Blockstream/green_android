package com.blockstream.green.ui.drawer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.blockstream.common.Urls
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.DrawerFragmentBinding
import com.blockstream.green.ui.settings.AppSettingsFragment
import com.blockstream.green.ui.wallet.AbstractWalletsFragment
import com.blockstream.green.ui.wallet.WalletsViewModel
import com.blockstream.green.utils.openBrowser
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.migration.OptionalInject

@OptionalInject
@AndroidEntryPoint
class DrawerFragment : AbstractWalletsFragment<DrawerFragmentBinding>(R.layout.drawer_fragment, menuRes = 0) {
    override val screenName: String? = null

    override val isDrawer: Boolean = true

    val viewModel: WalletsViewModel by viewModels()

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