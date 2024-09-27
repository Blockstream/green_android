package com.blockstream.compose.screens.devices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.hw_matrix_bg
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_create_a_pin
import blockstream_green.common.generated.resources.id_enter_and_confirm_a_unique_pin
import blockstream_green.common.generated.resources.id_setup_your_jade
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.models.devices.DeviceInfoViewModel
import com.blockstream.common.models.devices.DeviceInfoViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.sheets.EnvironmentBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.MenuBottomSheet
import com.blockstream.compose.sheets.MenuEntry
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.DeviceHandleSideEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class DeviceInfoScreen(val deviceId: String) : Screen, Parcelable {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DeviceInfoViewModel> {
            parametersOf(deviceId)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        DeviceInfoScreen(viewModel = viewModel)
    }

    companion object {
        @Composable
        fun getResult(fn: (String) -> Unit) = getNavigationResult(this::class, fn)

        internal fun setResult(result: String) =
            setNavigationResult(this::class, result)
    }
}


@Composable
fun DeviceInfoScreen(
    viewModel: DeviceInfoViewModelAbstract,
) {

    EnvironmentBottomSheet.getResult { result ->
        viewModel.postEvent(
            DeviceInfoViewModel.LocalEvents.SelectEnviroment(when(result) {
                -1 -> null
                0  -> false
                else -> true
            })
        )
    }

    var channels by remember { mutableStateOf<List<String>?>(null) }

    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    DeviceHandleSideEffect(viewModel = viewModel) {
        if(it is DeviceInfoViewModel.LocalSideEffects.SelectFirmwareChannel) {
            channels = it.channels
            bottomSheetNavigator?.show(
                MenuBottomSheet(
                    title = "Select Firmware Channel", entries = it.channels.map {
                        MenuEntry(
                            title = it,
                        )
                    }
                )
            )
        }
    }

    MenuBottomSheet.getResult {
        channels?.getOrNull(it)?.also { channel ->
            viewModel.postEvent(
                DeviceInfoViewModel.LocalEvents.AuthenticateAndContinue(
                    updateFirmwareFromChannel = channel
                )
            )
        }
    }

    val device = viewModel.device
    val jadeIsUninitialized by viewModel.jadeIsUninitialized.collectAsStateWithLifecycle()
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()


    Column {

        Column(
            modifier = Modifier.weight(4f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(modifier = Modifier.weight(1f)) {
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = device.name, style = titleMedium, textAlign = TextAlign.Center)
                Text(text = device.manufacturer ?: "", style = bodyLarge, textAlign = TextAlign.Center)
            }
        }

        GreenColumn(space = 8, modifier = Modifier.weight(5f)) {

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {

                androidx.compose.animation.AnimatedVisibility(
                    visible = onProgress,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(120.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = !onProgress && jadeIsUninitialized,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    GreenCard(
                        modifier = Modifier.align(Alignment.Center),
                        border = BorderStroke(1.dp, green)
                    ) {
                        GreenColumn(
                            padding = 0,
                            space = 4,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(Res.string.id_setup_your_jade).uppercase(),
                                style = labelMedium,
                                color = green,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(Res.string.id_create_a_pin),
                                style = labelLarge,
                                color = whiteHigh,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(Res.string.id_enter_and_confirm_a_unique_pin),
                                style = bodyMedium,
                                color = whiteMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !onProgress) {
                GreenButton(
                    text = stringResource(Res.string.id_continue),
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.postEvent(DeviceInfoViewModel.LocalEvents.AuthenticateAndContinue())
                    }
                )
            }

            AnimatedVisibility(visible = !onProgress) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    AppSettingsButton {
                        viewModel.postEvent(NavigateDestinations.AppSettings)
                    }
                }
            }
        }
    }
}