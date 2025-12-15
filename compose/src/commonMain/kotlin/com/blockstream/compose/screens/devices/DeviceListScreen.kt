package com.blockstream.compose.screens.devices

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.ble
import blockstream_green.common.generated.resources.blockstream_devices
import blockstream_green.common.generated.resources.id_blockstream_needs_access_to
import blockstream_green.common.generated.resources.id_choose_a_usb_or_bluetooth
import blockstream_green.common.generated.resources.id_connect_using_usb_or_bluetooth
import blockstream_green.common.generated.resources.id_connect_via_qr
import blockstream_green.common.generated.resources.id_enable_bluetooth
import blockstream_green.common.generated.resources.id_enable_location_services
import blockstream_green.common.generated.resources.id_follow_the_instructions_of_your
import blockstream_green.common.generated.resources.id_follow_the_instructions_on_jade
import blockstream_green.common.generated.resources.id_give_bluetooth_permissions
import blockstream_green.common.generated.resources.id_hold_the_green_button_on_the
import blockstream_green.common.generated.resources.id_looking_for_device
import blockstream_green.common.generated.resources.id_more_info
import blockstream_green.common.generated.resources.id_power_on_jade
import blockstream_green.common.generated.resources.id_select_initialize_and_choose_to
import blockstream_green.common.generated.resources.id_step_1s
import blockstream_green.common.generated.resources.id_troubleshoot
import blockstream_green.common.generated.resources.ledger_trezor
import blockstream_green.common.generated.resources.usb
import com.blockstream.data.devices.GreenDevice
import com.blockstream.data.managers.BluetoothState
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.models.devices.AbstractDeviceViewModel
import com.blockstream.compose.models.devices.DeviceListViewModel
import com.blockstream.compose.models.devices.DeviceListViewModelAbstract
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeviceListItem(device: GreenDevice, modifier: Modifier, onClick: () -> Unit) {
    GreenCard(onClick = onClick, padding = 0, modifier = modifier) {
        Image(
            painter = painterResource(device.icon()),
            modifier = Modifier.align(Alignment.CenterEnd).alpha(0.75f).height(120.dp)
                .aspectRatio(1f, matchHeightConstraintsFirst = true).padding(end = 8.dp),
            contentDescription = null
        )

        GreenRow(
            space = 16,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Image(
                painter = painterResource(if (device.isUsb) Res.drawable.usb else Res.drawable.ble),
                modifier = Modifier.size(24.dp),
                contentDescription = null
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, color = whiteHigh, style = labelLarge)
                Text(text = device.manufacturer ?: "", color = whiteMedium, style = bodyMedium)
            }
        }

    }
}

@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModelAbstract,
) {
    val isJade = viewModel.isJade
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()

    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    NavigateDestinations.AskJadeUnlock.getResult<Boolean> { isUnlocked ->
        if (isUnlocked) {
            viewModel.postEvent(DeviceListViewModel.LocalEvents.ConnectViaQRUnlocked)
        } else {
            viewModel.postEvent(DeviceListViewModel.LocalEvents.ConnectViaQRPinUnlock)
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.postEvent(AbstractDeviceViewModel.LocalEvents.Refresh)
            delay(1000)
            isRefreshing = false
        }
    }

    SetupScreen(viewModel = viewModel, withPadding = false, onProgressStyle = OnProgressStyle.Disabled) {

        PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = {
            isRefreshing = true
        }, state = pullToRefreshState) {

            Column {

                if (devices.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices) {
                            DeviceListItem(it, modifier = Modifier.padding(horizontal = 16.dp)) {
                                viewModel.postEvent(DeviceListViewModel.LocalEvents.SelectDevice(it))
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            if (isJade) {
                                val pagerState = rememberPagerState(pageCount = { 3 })

                                LaunchedEffect(Unit) {
                                    while (true) {
                                        delay(3000L)
                                        pagerState.animateScrollToPage((pagerState.currentPage + 1).takeIf { it < 3 }
                                            ?: 0)
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    Image(
                                        painter = painterResource(Res.drawable.blockstream_devices),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.weight(1f)
                                ) { page ->
                                    val title: String
                                    val subtitle: String

                                    when (page) {
                                        0 -> {
                                            title = stringResource(Res.string.id_power_on_jade)
                                            subtitle =
                                                stringResource(Res.string.id_hold_the_green_button_on_the)
                                        }

                                        1 -> {
                                            title =
                                                stringResource(Res.string.id_follow_the_instructions_on_jade)
                                            subtitle =
                                                stringResource(Res.string.id_select_initialize_and_choose_to)
                                        }

                                        else -> {
                                            title =
                                                stringResource(Res.string.id_connect_using_usb_or_bluetooth)
                                            subtitle =
                                                stringResource(Res.string.id_choose_a_usb_or_bluetooth)
                                        }
                                    }

                                    GreenColumn(
                                        space = 0,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {

                                        GreenCard {
                                            GreenColumn(
                                                padding = 0,
                                                space = 4,
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = stringResource(
                                                        Res.string.id_step_1s,
                                                        (page + 1).toString()
                                                    ),
                                                    color = green,
                                                    style = labelMedium
                                                )

                                                Text(
                                                    text = title,
                                                    color = whiteHigh,
                                                    style = labelLarge,
                                                    textAlign = TextAlign.Center
                                                )

                                                Text(
                                                    text = subtitle,
                                                    color = whiteMedium,
                                                    style = bodyMedium,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {

                                Box(modifier = Modifier.weight(1f)) {
                                    Image(
                                        painter = painterResource(Res.drawable.ledger_trezor),
                                        contentDescription = null
                                    )
                                }

                                Text(
                                    stringResource(Res.string.id_follow_the_instructions_of_your),
                                    color = whiteHigh,
                                    style = titleMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(
                                8.dp,
                                Alignment.CenterVertically
                            ),
                            modifier = Modifier.weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
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
                                    stringResource(Res.string.id_blockstream_needs_access_to),
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


                            if (bluetoothState == BluetoothState.ON) {
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

                if (isJade) {

                    GreenColumn(
                        space = 8,
                        padding = 0,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                    ) {

                        GreenButton(
                            text = stringResource(Res.string.id_connect_via_qr),
                            color = GreenButtonColor.WHITE,
                            type = GreenButtonType.OUTLINE,
                            size = GreenButtonSize.BIG,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.postEvent(DeviceListViewModel.LocalEvents.ConnectViaQR)
                            }
                        )

                        GreenButton(
                            text = stringResource(Res.string.id_troubleshoot),
                            color = GreenButtonColor.GREEN,
                            type = GreenButtonType.TEXT,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.postEvent(AbstractDeviceViewModel.LocalEvents.Troubleshoot)
                            }
                        )
                    }
                }
            }
        }
    }
}