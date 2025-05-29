package com.blockstream.common.models.camera

import com.blockstream.common.data.ScanResult
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.models.abstract.AbstractScannerViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.utils.Loggable

abstract class CameraViewModelAbstract(
    isDecodeContinuous: Boolean = false,
    val parentScreenName: String? = null,
    val setupArgs: SetupArgs? = null
) : AbstractScannerViewModel(isDecodeContinuous = isDecodeContinuous) {
    override fun screenName(): String = "Scan"

}

class CameraViewModel(isDecodeContinuous: Boolean = false, parentScreenName: String? = null, setupArgs: SetupArgs? = null) :
    CameraViewModelAbstract(isDecodeContinuous = isDecodeContinuous, parentScreenName = parentScreenName, setupArgs = setupArgs) {

    init {
        bootstrap()
    }

    override fun setScanResult(scanResult: ScanResult) {
        countly.qrScan(session = session, setupArgs = setupArgs, screenName = parentScreenName)
        postSideEffect(SideEffects.Success(scanResult))
    }

    companion object : Loggable()
}

class CameraViewModelPreview() : CameraViewModelAbstract() {

    override fun setScanResult(scanResult: ScanResult) {

    }

    companion object {
        fun preview() = CameraViewModelPreview()
    }
}