package com.blockstream.compose.screens.onboarding.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_pins_do_not_match_please_try
import blockstream_green.common.generated.resources.id_set_a_pin
import blockstream_green.common.generated.resources.id_verify_your_pin
import blockstream_green.common.generated.resources.id_youll_need_your_pin_to_log_in
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.onboarding.phone.PinViewModel
import com.blockstream.common.models.onboarding.phone.PinViewModelAbstract
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.components.ScreenContainer
import com.blockstream.compose.dialogs.LightningShortcutDialog
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.views.PinView
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class PinScreen(val setupArgs: SetupArgs) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<PinViewModel> {
            parametersOf(setupArgs)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        PinScreen(viewModel = viewModel)
    }
}

@Composable
fun PinScreen(
    viewModel: PinViewModelAbstract
) {
    var pin by remember { mutableStateOf("") }
    var isVerify by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current

    val rocketAnimation by viewModel.rocketAnimation.collectAsStateWithLifecycle()
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val onProgressDescription by viewModel.onProgressDescription.collectAsStateWithLifecycle()

    var lightningShortcutViewModel by remember {
        mutableStateOf<GreenViewModel?>(null)
    }

    lightningShortcutViewModel?.also {
        LightningShortcutDialog(viewModel = it) {
            viewModel.postEvent(Events.Continue)
            lightningShortcutViewModel = null
        }
    }

    HandleSideEffect(viewModel = viewModel) {
        if(it is SideEffects.LightningShortcut) {
            lightningShortcutViewModel = SimpleGreenViewModel(viewModel.greenWallet)
        }
    }

    ScreenContainer(
        onProgress = onProgress,
        onProgressDescription = onProgressDescription,
        blurBackground = !rocketAnimation,
        riveAnimation = if (viewModel.setupArgs.isRestoreFlow) RiveAnimation.ROCKET else null
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GreenColumn(padding = 0, space = 8) {
                Text(
                    stringResource(if (isVerify) Res.string.id_verify_your_pin else Res.string.id_set_a_pin),
                    style = displayMedium
                )
                Text(
                    stringResource(Res.string.id_youll_need_your_pin_to_log_in),
                    style = bodyLarge
                )
            }

            PinView(modifier = Modifier.weight(1f), isVerifyMode = true, onPinNotVerified = {
                scope.launch {
                    snackbar.showSnackbar(getString(Res.string.id_pins_do_not_match_please_try))
                }
            }, onModeChange = {
                isVerify = it
            }) {
                pin = it
            }

            GreenButton(
                stringResource(Res.string.id_continue),
                size = GreenButtonSize.BIG,
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.isNotBlank()
            ) {
                viewModel.postEvent(PinViewModel.LocalEvents.SetPin(pin))
            }
        }
    }
}
