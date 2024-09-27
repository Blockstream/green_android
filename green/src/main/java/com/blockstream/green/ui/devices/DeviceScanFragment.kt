package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.devices.AbstractDeviceViewModel
import com.blockstream.common.models.devices.DeviceScanViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.devices.DeviceScanScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class DeviceScanFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = 0
) {
    private val args: DeviceScanFragmentArgs by navArgs()

    val viewModel: DeviceScanViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override val useCompose: Boolean = true

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val title: String?
        get() = viewModel.greenWalletOrNull?.name

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if (sideEffect is AbstractDeviceViewModel.LocalSideEffects.RequestWalletIsDifferent) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.id_warning)
                .setMessage("The wallet hash is different from the previous wallet. Continue with a new Ephemeral Wallet?")
                .setPositiveButton(R.string.id_continue) { _, _ ->
                    viewModel.requestUserActionEmitter?.complete(true)
                }.setNegativeButton(android.R.string.cancel) { _, _ ->
                    viewModel.requestUserActionEmitter?.complete(false)
                }
                .setOnDismissListener {
                    viewModel.requestUserActionEmitter?.complete(false)
                }
                .show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    DeviceScanScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun updateToolbar() {
        super.updateToolbar()
        toolbar.setButton(button = getString(R.string.id_troubleshoot)) {
            openBrowser(Urls.JADE_TROUBLESHOOT)
        }
    }
}