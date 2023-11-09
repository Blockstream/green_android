package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.ScanResult
import com.blockstream.common.models.add.ExportLightningKeyViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.compose.screens.lightning.ExportLightningKeyScreen
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class ExportLightningKeyFragment : AbstractAddAccountFragment<ComposeViewBinding>(R.layout.compose_view, menuRes = 0) {
    private val args: ExportLightningKeyFragmentArgs by navArgs()

    override val viewModel: ExportLightningKeyViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override val assetId: String = BTC_POLICY_ASSET

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if(sideEffect is ExportLightningKeyViewModel.LocalSideEffects.ScanQr){
            CameraBottomSheetDialogFragment.showSingle(viewModel.screenName(), decodeContinuous = true, fragmentManager = childFragmentManager)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<ScanResult>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                result.bcur?.encrypted?.also {
                    viewModel.postEvent(ExportLightningKeyViewModel.LocalEvents.JadeBip8539Reply(result.bcur?.publicΚey ?: "", it))
                }
            }
        }

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                GreenTheme {
                    ExportLightningKeyScreen(viewModel)
                }
            }
        }
    }
}