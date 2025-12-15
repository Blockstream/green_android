package com.blockstream.data

import okio.FileSystem

actual fun platformFileSystem(): FileSystem {
    return FileSystem.SYSTEM
}

actual fun platformName() = "desktop"
