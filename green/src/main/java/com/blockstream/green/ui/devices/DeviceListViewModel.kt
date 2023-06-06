package com.blockstream.green.ui.devices

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockstream.common.gdk.device.DeviceBrand
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceConnectionManager
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.QATester
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class DeviceListViewModel @AssistedInject constructor(
    countly: Countly,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    deviceManager: DeviceManager,
    qaTester: QATester,
    @Assisted val isJade: Boolean
) : AbstractDeviceViewModel(sessionManager, walletRepository, deviceManager, qaTester, countly, null) {

    val devices = MutableLiveData(listOf<Device>())
    val hasBleConnectivity = true // deviceBrand == null || deviceBrand.hasBleConnectivity

    var onSuccess: (() -> Unit)? = null

    val canEnableBluetooth = MutableLiveData(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)

    override val device: Device? = null

    override val deviceConnectionManagerOrNull: DeviceConnectionManager? = null

    init {
        deviceManager
            .devicesStateFlow.map { devices ->
                devices.filter {
                    (it.deviceBrand == DeviceBrand.Blockstream && isJade) || (it.deviceBrand != DeviceBrand.Blockstream && !isJade)
                }
            }
            .onEach {
                devices.value = it
            }.launchIn(viewModelScope)
    }

    fun askForPermissionOrBond(device: Device) {
        device.askForPermissionOrBond(onSuccess = {
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(device)))
        }, onError = {error ->
            error?.also { onError.postValue(ConsumableEvent(it)) }
        })
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            isJade: Boolean
        ): DeviceListViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            isJade: Boolean
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(isJade) as T
            }
        }
    }
}