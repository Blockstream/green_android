package com.blockstream.common

import okio.FileSystem

expect fun platformFileSystem(): FileSystem

expect fun platformName(): String