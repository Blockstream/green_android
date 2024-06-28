package com.blockstream.green.ui.devices

import androidx.lifecycle.MutableLiveData
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.devices.AndroidDevice
import com.blockstream.green.devices.DeviceConnectionManager
import com.blockstream.green.devices.toAndroidDevice
import com.blockstream.green.utils.QATester
import com.blockstream.green.utils.isDevelopmentOrDebug
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class DeviceListViewModel constructor(
    deviceManager: DeviceManager,
    qaTester: QATester,
    @InjectedParam val isJade: Boolean
) : AbstractDeviceViewModel(deviceManager, qaTester, null) {

    val devices = MutableLiveData(listOf<GreenDevice>())
    val hasBleConnectivity = true

    override val device: AndroidDevice? = null

    override val deviceConnectionManagerOrNull: DeviceConnectionManager? = null

    val isDevelopment = isDevelopmentOrDebug

    init {
        deviceManager.devices.map { devices ->
            devices.filter { it.isJade == isJade }
        }.onEach {
            devices.value = it
        }.launchIn(this)

        bootstrap()
    }

    fun askForPermission(device: GreenDevice) {
        device.toAndroidDevice()?.askForPermission(onSuccess = {
            postSideEffect(SideEffects.Navigate(device))
        }, onError = { error ->
            error?.also {
                postSideEffect(SideEffects.ErrorSnackbar(it))
            }
        })
    }
}