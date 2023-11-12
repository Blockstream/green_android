package com.blockstream.common.models.camera

import com.blockstream.common.models.GreenViewModel


abstract class CameraViewModelAbstract() : GreenViewModel() {
    override fun screenName(): String = "Scan"

}

class CameraViewModel() : CameraViewModelAbstract() {

    init {
        bootstrap()
    }
}

class CameraViewModelPreview() : CameraViewModelAbstract() {

    companion object {
        fun preview() = CameraViewModelPreview()
    }
}