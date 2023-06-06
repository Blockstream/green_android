package com.blockstream.common

import okio.FileSystem
interface Platform {
    val name: String
}

expect fun platformFileSystem(): FileSystem

expect fun getPlatform(): Platform