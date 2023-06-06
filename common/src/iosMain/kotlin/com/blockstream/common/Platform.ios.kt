package com.blockstream.common

import okio.FileSystem
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun platformFileSystem(): FileSystem { return FileSystem.SYSTEM }

actual fun getPlatform(): Platform = IOSPlatform()