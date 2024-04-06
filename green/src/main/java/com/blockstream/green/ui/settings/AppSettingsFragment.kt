package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.settings.AppSettingsViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.settings.AppSettingsScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel


class AppSettingsFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view) {
    private val viewModel: AppSettingsViewModel by viewModel()

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val sideEffectsHandledByAppFragment: Boolean = false

    override val useCompose: Boolean = true

    private val onBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            viewModel.postEvent(AppSettingsViewModel.LocalEvents.OnBack)
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
                    AppSettingsScreen(viewModel = viewModel)
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    fun navigateUp(){
        onBackCallback.handleOnBackPressed()
    }
}
