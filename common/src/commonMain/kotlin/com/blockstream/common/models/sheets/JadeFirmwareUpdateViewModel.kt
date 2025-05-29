package com.blockstream.common.models.sheets

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_firmware_version_s
import blockstream_green.common.generated.resources.id_hash_s
import com.blockstream.common.devices.DeviceState
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.padHex
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.jade.firmware.FirmwareUpdateState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class JadeFirmwareUpdateViewModelAbstract() : GreenViewModel() {

    @NativeCoroutinesState
    abstract val status: StateFlow<FirmwareUpdateState?>

    @NativeCoroutinesState
    abstract val firmware: StateFlow<String>

    @NativeCoroutinesState
    abstract val hash: StateFlow<String>

    @NativeCoroutinesState
    abstract val progress: StateFlow<Int>

    @NativeCoroutinesState
    abstract val transfer: StateFlow<String>
}

class JadeFirmwareUpdateViewModel(
    deviceId: String? = null
) : JadeFirmwareUpdateViewModelAbstract() {

    private val deviceManager: DeviceManager by inject()

    override var deviceOrNull = deviceManager.getDevice(deviceId)

    override val status: StateFlow<FirmwareUpdateState?> = deviceOrNull?.firmwareState ?: MutableStateFlow(null)

    private val _firmware = MutableStateFlow("")
    override val firmware: StateFlow<String> = _firmware

    private val _hash = MutableStateFlow("")
    override val hash: StateFlow<String> = _hash

    private val _progress = MutableStateFlow(0)
    override val progress: StateFlow<Int> = _progress

    private val _transfer = MutableStateFlow("")
    override val transfer: StateFlow<String> = _transfer

    init {

        deviceOrNull?.also { device ->
            device.deviceState.onEach {
                if (it == DeviceState.DISCONNECTED) {
                    postSideEffect(SideEffects.Dismiss)
                }
            }.launchIn(this)

            device.firmwareState.onEach {
                when (it) {
                    is FirmwareUpdateState.Initiate -> {
                        _firmware.value = getString(
                            Res.string.id_firmware_version_s,
                            "${it.firmwareFileData.image.version} ${it.firmwareFileData.image.config}"
                        )
                        _hash.value = getString(Res.string.id_hash_s, it.hash.padHex() ?: "")
                    }

                    is FirmwareUpdateState.Uploaded -> {
                        _progress.value = 100

                        delay(3000L)
                        postSideEffect(SideEffects.Dismiss)
                    }

                    is FirmwareUpdateState.Uploading -> {
                        _progress.value = ((it.written / it.totalSize.toFloat()) * 100).toInt()
                        _transfer.value = "${it.written} / ${it.totalSize}"
                    }

                    is FirmwareUpdateState.Failed -> {
                        postSideEffect(SideEffects.Dismiss)
                    }

                    is FirmwareUpdateState.Completed -> {
                        postSideEffect(SideEffects.Dismiss)
                    }

                    null -> {
                        postSideEffect(SideEffects.Dismiss)
                    }
                }

            }.launchIn(this)
        } ?: run {
            postSideEffect(SideEffects.Dismiss)
        }

        bootstrap()
    }
}

class JadeFirmwareUpdateViewModelPreview : JadeFirmwareUpdateViewModelAbstract() {

    override val status: StateFlow<FirmwareUpdateState?> = MutableStateFlow(null)
    override val firmware: StateFlow<String> = MutableStateFlow("Firmware Version: 1.0.0")
    override val hash: StateFlow<String> = MutableStateFlow("Hash: qwertyuiopasdfghjklzxcvbnm")
    override val transfer = MutableStateFlow("1000 / 2000")
    override val progress = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            do {
                delay(250L)
                progress.value += 1
                transfer.value = "${progress.value} / 100"
            } while (progress.value < 100)
        }
    }

    companion object {
        fun preview() = JadeFirmwareUpdateViewModelPreview()
    }
}