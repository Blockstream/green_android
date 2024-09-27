package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.devices.DeviceListViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.devices.DeviceListScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class DeviceListFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = 0
) {
    private val args: DeviceListFragmentArgs by navArgs()

    val viewModel: DeviceListViewModel by viewModel {
        parametersOf(args.isJade)
    }

    override val useCompose: Boolean = true

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    DeviceListScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun updateToolbar() {
        super.updateToolbar()

        if (args.isJade) {
            toolbar.setButton(button = getString(R.string.id_setup_guide)) {
                viewModel.postEvent(NavigateDestinations.JadeGuide)
            }
        }
    }

}