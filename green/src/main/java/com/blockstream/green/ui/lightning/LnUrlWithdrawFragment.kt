package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.lightning.LnUrlWithdrawViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.lightning.LnUrlWithdrawScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LnUrlWithdrawFragment : AppFragment<ComposeViewBinding>(
    R.layout.compose_view,
    menuRes = 0
) {

    val args: LnUrlWithdrawFragmentArgs by navArgs()

    val requestData
        get() = args.lnUrlWithdrawRequest.deserialize()

    override val subtitle: String
        get() = viewModel.greenWallet.name

    override val toolbarIcon: Int
        get() = R.drawable.ic_lightning

    val viewModel: LnUrlWithdrawViewModel by viewModel {
        parametersOf(args.wallet, requestData)
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
                    LnUrlWithdrawScreen(viewModel = viewModel)
                }
            }
        }
    }
}
