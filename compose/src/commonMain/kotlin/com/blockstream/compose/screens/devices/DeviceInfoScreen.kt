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
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_create_a_pin
import blockstream_green.common.generated.resources.id_enter_and_confirm_a_unique_pin
import blockstream_green.common.generated.resources.id_setup_your_jade
import com.blockstream.common.data.MenuEntry
import com.blockstream.common.data.MenuEntryList
import com.blockstream.common.events.Events
import com.blockstream.common.models.devices.DeviceInfoViewModel
import com.blockstream.common.models.devices.DeviceInfoViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.getResult
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeviceInfoScreen(
    viewModel: DeviceInfoViewModelAbstract,
) {

    NavigateDestinations.Environment.getResult<Int> { result: Int ->
        viewModel.postEvent(
            DeviceInfoViewModel.LocalEvents.SelectEnviroment(
                when (result) {
                    -1 -> null
                    0 -> false
                    else -> true
                }
            )
        )
    }

    NavigateDestinations.NewJadeConnected.getResult<Boolean> {
        viewModel.postEvent(
            NavigateDestinations.JadeGenuineCheck(
                greenWalletOrNull = viewModel.greenWalletOrNull,
                deviceId = viewModel.deviceId
            )
        )
    }

    NavigateDestinations.JadeGenuineCheck.getResult<Boolean> {
        if (it) {
            viewModel.postEvent(DeviceInfoViewModel.LocalEvents.GenuineCheckSuccess)
        }
    }

    // Device Passphrase
    NavigateDestinations.DevicePassphrase.getResult<String> {
        viewModel.postEvent(Events.DeviceRequestResponse(it))
    }

    // Device PinMatrix
    NavigateDestinations.DevicePin.getResult<String> {
        viewModel.postEvent(Events.DeviceRequestResponse(it))
    }

    var channels by remember { mutableStateOf<List<String>?>(null) }

    NavigateDestinations.Menu.getResult<Int> {
        channels?.getOrNull(it)?.also { channel ->
            viewModel.postEvent(
                DeviceInfoViewModel.LocalEvents.AuthenticateAndContinue(
                    updateFirmwareFromChannel = channel
                )
            )
        }
    }

    val device = viewModel.deviceOrNull
    val jadeIsUninitialized by viewModel.jadeIsUninitialized.collectAsStateWithLifecycle()
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel, withPadding = false, onProgressStyle = OnProgressStyle.Disabled, sideEffectsHandler = {
        if (it is DeviceInfoViewModel.LocalSideEffects.SelectFirmwareChannel) {
            channels = it.channels
            viewModel.postEvent(
                NavigateDestinations.Menu(
                    title = "Select Firmware Channel",
                    entries = MenuEntryList(it.channels.map {
                        MenuEntry(
                            title = it,
                        )
                    })
                )
            )
        }
    }) {

        Column(
            modifier = Modifier.weight(4f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(modifier = Modifier.weight(1f)) {
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
                Text(text = device?.name ?: "", style = titleMedium, textAlign = TextAlign.Center)
                Text(text = device?.manufacturer ?: "", style = bodyLarge, textAlign = TextAlign.Center)
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