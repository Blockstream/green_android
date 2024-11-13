package com.blockstream.compose.screens.devices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import blockstream_green.common.generated.resources.id_blockstream_green_needs_access
import blockstream_green.common.generated.resources.id_connect_usb_cable_or_enable
import blockstream_green.common.generated.resources.id_enable_bluetooth
import blockstream_green.common.generated.resources.id_enable_location_services
import blockstream_green.common.generated.resources.id_give_bluetooth_permissions
import blockstream_green.common.generated.resources.id_looking_for_device
import blockstream_green.common.generated.resources.id_more_info
import blockstream_green.common.generated.resources.id_unlock_your_device_to_continue
import blockstream_green.common.generated.resources.question
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.managers.BluetoothState
import com.blockstream.common.models.devices.AbstractDeviceViewModel
import com.blockstream.common.models.devices.DeviceScanViewModel
import com.blockstream.common.models.devices.DeviceScanViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.DeviceHandleSideEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class DeviceScanScreen(val greenWallet: GreenWallet) : Screen, Parcelable {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DeviceScanViewModel> {
            parametersOf(greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        DeviceScanScreen(viewModel = viewModel)
    }

    companion object {
        @Composable
        fun getResult(fn: (String) -> Unit) = getNavigationResult(this::class, fn)

        internal fun setResult(result: String) =
            setNavigationResult(this::class, result)
    }
}


@Composable
fun DeviceScanScreen(
    viewModel: DeviceScanViewModelAbstract,
) {

    val device by viewModel.deviceFlow.collectAsStateWithLifecycle()
    val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()

    DeviceHandleSideEffect(viewModel = viewModel)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Box(modifier = Modifier.weight(1f)) {

            Image(
                painter = painterResource(Res.drawable.hw_matrix_bg),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center)
            )

            Image(
                painter = painterResource(device?.icon() ?: viewModel.greenWallet.deviceIdentifiers?.firstOrNull()?.brand?.icon() ?: Res.drawable.question),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(
            text = stringResource(if (device == null) Res.string.id_connect_usb_cable_or_enable else Res.string.id_unlock_your_device_to_continue),
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
            minLines = 2,
            style = titleLarge,
            color = whiteHigh
        )

        Column(
            modifier = Modifier.weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                8.dp,
                Alignment.CenterVertically
            ),
        ) {

            if (bluetoothState == BluetoothState.OFF) {
                GreenButton(
                    text = stringResource(Res.string.id_enable_bluetooth),
                    onClick = {
                        viewModel.postEvent(AbstractDeviceViewModel.LocalEvents.EnableBluetooth)
                    }
                )
            }

            if (bluetoothState == BluetoothState.PERMISSIONS_NOT_GRANTED) {
                GreenButton(
                    text = stringResource(Res.string.id_give_bluetooth_permissions),
                    onClick = {
                        viewModel.postEvent(AbstractDeviceViewModel.LocalEvents.AskForBluetoothPermissions)
                    }
                )
            }


            if (bluetoothState == BluetoothState.LOCATION_SERVICES_DISABLED) {
                GreenButton(
                    text = stringResource(Res.string.id_enable_location_services),
                    onClick = {
                        viewModel.postEvent(AbstractDeviceViewModel.LocalEvents.EnableLocationService)
                    }
                )
            }

            if (bluetoothState == BluetoothState.LOCATION_SERVICES_DISABLED || bluetoothState == BluetoothState.PERMISSIONS_NOT_GRANTED) {

                Text(
                    stringResource(Res.string.id_blockstream_green_needs_access),
                    color = whiteMedium,
                    style = bodySmall,
                    textAlign = TextAlign.Center
                )


                GreenButton(
                    text = stringResource(Res.string.id_more_info),
                    color = GreenButtonColor.GREEN,
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.SMALL,
                    onClick = {
                        viewModel.postEvent(AbstractDeviceViewModel.LocalEvents.LocationServiceMoreInfo)
                    }
                )
            }

            AnimatedVisibility(visible = device == null && bluetoothState == BluetoothState.ON) {
                GreenColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        strokeWidth = 1.dp,
                        modifier = Modifier
                            .size(24.dp),
                    )


                    Text(
                        stringResource(Res.string.id_looking_for_device),
                        color = whiteHigh,
                        style = labelLarge
                    )
                }
            }
        }
    }
}