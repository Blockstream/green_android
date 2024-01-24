package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.hardware.UseHardwareDeviceViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.onboarding.hardware.UseHardwareDeviceScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class UseHardwareDeviceFragment : AppFragment<ComposeViewBinding>(
    R.layout.compose_view
) {
    val viewModel: UseHardwareDeviceViewModel by viewModel()

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val sideEffectsHandledByAppFragment: Boolean = false

    override val useCompose: Boolean = true

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if(sideEffect is SideEffects.NavigateTo){
            (sideEffect.destination as? NavigateDestinations.DeviceList)?.also {
                navigate(UseHardwareDeviceFragmentDirections.actionGlobalDeviceListFragment(isJade = it.isJade))
            }
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
                    UseHardwareDeviceScreen(viewModel = viewModel)
                }
            }
        }
    }
}