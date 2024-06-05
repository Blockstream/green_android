package com.blockstream.green.ui.archived


import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.archived.ArchivedAccountsViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.archived.ArchivedAccountsScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ArchivedAccountsFragment :
    AppFragment<ComposeViewBinding>(R.layout.compose_view, 0) {
    val args: ArchivedAccountsFragmentArgs by navArgs()

    val viewModel: ArchivedAccountsViewModel by viewModel {
        parametersOf(args.wallet, args.navigateToOverview)
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
                    ArchivedAccountsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
