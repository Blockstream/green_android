package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.add.ChooseAccountTypeViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.add.ChooseAccountTypeScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ChooseAccountTypeFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, 0) {
    val args: ChooseAccountTypeFragmentArgs by navArgs()

    val viewModel: ChooseAccountTypeViewModel by viewModel {
        parametersOf(args.wallet, args.asset?.let { AssetBalance(it) }, args.isReceive)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val useCompose: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    ChooseAccountTypeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
