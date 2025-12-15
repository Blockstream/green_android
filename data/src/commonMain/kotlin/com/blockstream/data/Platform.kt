package com.blockstream.data

import okio.FileSystem

expect fun platformFileSystem(): FileSystem

expect fun platformName(): String