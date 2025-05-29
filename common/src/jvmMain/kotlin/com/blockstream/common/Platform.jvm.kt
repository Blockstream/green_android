package com.blockstream.common

import okio.FileSystem

actual fun platformFileSystem(): FileSystem {
    return FileSystem.SYSTEM
}

actual fun platformName() = "desktop"
