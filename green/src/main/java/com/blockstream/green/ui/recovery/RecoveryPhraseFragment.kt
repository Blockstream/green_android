package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.recovery.RecoveryPhraseViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.recovery.RecoveryPhraseScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RecoveryPhraseFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view
) {
    private val args: RecoveryPhraseFragmentArgs by navArgs()

    override val subtitle: String?
        get() = if(args.isLightning) getString(R.string.id_lightning) else null

    val viewModel: RecoveryPhraseViewModel by viewModel {
        parametersOf(args.isLightning, args.credentials, args.wallet)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    RecoveryPhraseScreen(viewModel = viewModel)
                }
            }
        }
    }
}