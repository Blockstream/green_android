package com.blockstream.green.ui.about

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockstream.common.models.about.*
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.about.AboutScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class AboutFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, menuRes = 0) {

    val viewModel: AboutViewModel by viewModel()

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
                    AboutScreen(viewModel = viewModel)
                }
            }
        }
    }
}