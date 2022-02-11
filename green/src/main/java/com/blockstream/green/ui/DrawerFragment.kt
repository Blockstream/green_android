package com.blockstream.green.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.databinding.DrawerFragmentBinding
import com.blockstream.green.ui.settings.AppSettingsDialogFragment
import com.blockstream.green.utils.openBrowser
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.migration.OptionalInject

@OptionalInject
@AndroidEntryPoint
class DrawerFragment : WalletListCommonFragment<DrawerFragmentBinding>(R.layout.drawer_fragment, menuRes = 0) {
    override val screenName: String = "Drawer" // Not used yet

    override val isDrawer: Boolean = true

    val viewModel: WalletListCommonViewModel by viewModels()
    private val activityViewModel: MainActivityViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel
        binding.activityVm = activityViewModel

        init(binding.common, viewModel)

        binding.buttonAppSettings.setOnClickListener {
            AppSettingsDialogFragment.show(childFragmentManager)
            closeDrawer()
        }

        binding.helpCenter.setOnClickListener {
            openBrowser(Urls.HELP_CENTER)
        }
    }
}