package com.blockstream.compose.screens.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.onboarding.PinViewModel
import com.blockstream.common.models.onboarding.PinViewModelAbstract
import com.blockstream.common.models.onboarding.PinViewModelPreview
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.AppBarData
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.views.PinView
import com.blockstream.compose.views.ScreenContainer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

@Parcelize
data class PinScreen(val setupArgs: SetupArgs) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<PinViewModel>() {
            parametersOf(setupArgs)
        }
        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        AppBar {
            AppBarData(hide = onProgress)
        }
        PinScreen(viewModel = viewModel)
    }
}

@Composable
fun PinScreen(
    viewModel: PinViewModelAbstract
) {
    HandleSideEffect(viewModel = viewModel)

    var pin by remember { mutableStateOf("") }
    var isVerify by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current

    val rocketAnimation by viewModel.rocketAnimation.collectAsStateWithLifecycle()
    ScreenContainer(viewModel = viewModel, blurBackground = !rocketAnimation, showRiveAnimation = rocketAnimation) {
        GreenColumn(space = 24) {
            GreenColumn(padding = 0, space = 8) {
                Text(
                    stringResource(id = if (isVerify) R.string.id_verify_your_pin else R.string.id_set_a_pin),
                    style = displayMedium
                )
                Text(
                    stringResource(id = R.string.id_youll_need_your_pin_to_log_in),
                    style = bodyLarge
                )
            }

            PinView(modifier = Modifier.weight(1f), isVerifyMode = true, onPinNotVerified = {
                scope.launch {
                    snackbar.showSnackbar(context.getString(R.string.id_pins_do_not_match_please_try))
                }
            }, onModeChange = {
                isVerify = it
            }) {
                pin = it
            }

            GreenButton(
                stringResource(R.string.id_continue),
                size = GreenButtonSize.BIG,
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.isNotBlank()
            ) {
                viewModel.postEvent(PinViewModel.LocalEvents.SetPin(pin))
            }
        }
    }
}

@Composable
@Preview
fun PinScreenPreview(
) {
    GreenTheme {
        PinScreen(viewModel = PinViewModelPreview.preview().also {
            it.onProgress.value = true
        })
    }
}