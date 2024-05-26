package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.about.AboutViewModel
import com.blockstream.common.models.lightning.LnUrlAuthViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.about.AboutScreen
import com.blockstream.compose.screens.lightning.LnUrlAuthScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.databinding.LnurlAuthFragmentBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class LnUrlAuthFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, menuRes = 0) {

    val args: LnUrlAuthFragmentArgs by navArgs()

    private val requestData by lazy { args.lnUrlAuthRequest.deserialize() }

    override val subtitle: String
        get() = args.wallet.name

    override val toolbarIcon: Int
        get() = R.drawable.ic_lightning

    val viewModel: LnUrlAuthViewModel by viewModel {
        parametersOf(
            args.wallet,
            requestData
        )
    }

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    LnUrlAuthScreen(viewModel = viewModel)
                }
            }
        }
    }
}
