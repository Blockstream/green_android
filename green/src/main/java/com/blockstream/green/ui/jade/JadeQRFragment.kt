package com.blockstream.green.ui.jade

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.jade.JadeQRViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.jade.JadeQRScreen
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.extensions.setNavigationResult
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class JadeQRFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, menuRes = 0) {

    val args: JadeQRFragmentArgs by navArgs()

    override val title: String?
        get() = if(args.isLightningMnemonicExport) getString(R.string.id_export_lightning_key_to_green) else null

    val viewModel: JadeQRViewModel by viewModel {
        parametersOf(args.isLightningMnemonicExport, args.wallet)
    }

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if (sideEffect is SideEffects.Mnemonic) {
            setNavigationResult(
                result = sideEffect.mnemonic,
                key = MNEMONIC_RESULT,
                destinationId = R.id.chooseAccountTypeFragment
            )
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
                    JadeQRScreen(viewModel)
                }
            }
        }
    }

    companion object {
        const val MNEMONIC_RESULT = "MNEMONIC_RESULT"
    }
}