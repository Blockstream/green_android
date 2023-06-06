package com.blockstream.common

import okio.FileSystem

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual fun platformFileSystem(): FileSystem { return FileSystem.SYSTEM }

actual fun getPlatform(): Platform = AndroidPlatform()