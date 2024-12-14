package com.blockstream.compose.screens.devices

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.hw_matrix_bg
import blockstream_green.common.generated.resources.id_authenticate_your_jade
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_confirm_on_your_device
import blockstream_green.common.generated.resources.id_contact_support
import blockstream_green.common.generated.resources.id_continue_as_diy
import blockstream_green.common.generated.resources.id_continue_with_jade
import blockstream_green.common.generated.resources.id_genuine_check_canceled
import blockstream_green.common.generated.resources.id_perform_a_genuine_check_to
import blockstream_green.common.generated.resources.id_retry
import blockstream_green.common.generated.resources.id_this_device_was_not_manufactured_by
import blockstream_green.common.generated.resources.id_this_jade_is_not_genuine
import blockstream_green.common.generated.resources.id_we_could_successfully_verify_your
import blockstream_green.common.generated.resources.id_we_were_unable_to_complete_the_genuine
import blockstream_green.common.generated.resources.id_your_jade_is_genuine
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.models.devices.JadeGenuineCheckViewModel
import com.blockstream.common.models.devices.JadeGenuineCheckViewModelAbstract
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class JadeGenuineCheckScreen(val greenWallet: GreenWallet?, val deviceId: String?) : Screen,
    Parcelable {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<JadeGenuineCheckViewModel> {
            parametersOf(greenWallet, deviceId)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        JadeGenuineCheckScreen(viewModel = viewModel)
    }

    companion object {
        @Composable
        fun getResult(fn: (Boolean) -> Unit) = getNavigationResult(this::class, fn)

        internal fun setResult(result: Boolean) =
            setNavigationResult(this::class, result)
    }
}


@Composable
fun JadeGenuineCheckScreen(
    viewModel: JadeGenuineCheckViewModelAbstract,
) {

    val device = viewModel.deviceOrNull
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val genuineState by viewModel.genuineState.collectAsStateWithLifecycle()

    HandleSideEffect(viewModel = viewModel) {
        when (it) {
            is SideEffects.Success -> {
                JadeGenuineCheckScreen.setResult(true)
                viewModel.postEvent(Events.NavigateBack)
            }
        }
    }

    Column {

        Box(modifier = Modifier.weight(4f).fillMaxWidth()) {
            Image(
                painter = painterResource(Res.drawable.hw_matrix_bg),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center)
            )

            Image(
                painter = painterResource(device.icon()),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        GreenColumn(space = 32, modifier = Modifier.weight(5f).fillMaxWidth()) {

            GreenColumn(
                padding = 0,
                space = 8,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).weight(1f)
            ) {
                when (genuineState) {
                    JadeGenuineCheckViewModel.GenuineState.GENUINE -> Res.string.id_your_jade_is_genuine
                    JadeGenuineCheckViewModel.GenuineState.CHECKING -> Res.string.id_authenticate_your_jade
                    JadeGenuineCheckViewModel.GenuineState.NOT_GENUINE -> Res.string.id_this_jade_is_not_genuine
                    else -> Res.string.id_genuine_check_canceled
                }.also {
                    Text(
                        text = stringResource(it),
                        style = titleLarge,
                        textAlign = TextAlign.Center
                    )
                }

                when (genuineState) {
                    JadeGenuineCheckViewModel.GenuineState.GENUINE -> Res.string.id_we_could_successfully_verify_your
                    JadeGenuineCheckViewModel.GenuineState.CHECKING -> Res.string.id_perform_a_genuine_check_to
                    JadeGenuineCheckViewModel.GenuineState.NOT_GENUINE -> Res.string.id_this_device_was_not_manufactured_by
                    else -> Res.string.id_we_were_unable_to_complete_the_genuine
                }.also {
                    Text(
                        text = stringResource(it),
                        style = bodyMedium,
                        textAlign = TextAlign.Center,
                        color = whiteMedium
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = onProgress,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                GreenColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(32.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    Text(
                        stringResource(Res.string.id_confirm_on_your_device),
                        style = labelLarge
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !onProgress,
            ) {

                if (genuineState == JadeGenuineCheckViewModel.GenuineState.GENUINE) {
                    GreenButton(
                        text = stringResource(Res.string.id_continue_with_jade),
                        size = GreenButtonSize.BIG,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.postEvent(Events.Continue)
                        }
                    )
                } else {
                    GreenColumn(
                        padding = 0,
                        space = 16,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GreenButton(
                            text = stringResource(if (genuineState == JadeGenuineCheckViewModel.GenuineState.NOT_GENUINE) Res.string.id_continue_as_diy else Res.string.id_cancel),
                            size = GreenButtonSize.BIG,
                            type = GreenButtonType.OUTLINE,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (genuineState == JadeGenuineCheckViewModel.GenuineState.NOT_GENUINE) {
                                    viewModel.postEvent(JadeGenuineCheckViewModel.LocalEvents.ContinueAsDIY)
                                } else {
                                    viewModel.postEvent(JadeGenuineCheckViewModel.LocalEvents.Cancel)
                                }

                            }
                        )

                        GreenButton(
                            text = stringResource(if (genuineState == JadeGenuineCheckViewModel.GenuineState.NOT_GENUINE) Res.string.id_contact_support else Res.string.id_retry),
                            size = GreenButtonSize.BIG,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (genuineState == JadeGenuineCheckViewModel.GenuineState.NOT_GENUINE) {
                                    viewModel.postEvent(JadeGenuineCheckViewModel.LocalEvents.ContactSupport)
                                } else {
                                    viewModel.postEvent(JadeGenuineCheckViewModel.LocalEvents.Retry)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}