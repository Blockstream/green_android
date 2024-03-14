package com.blockstream.common.data

class AppInfo constructor(
    val userAgent: String,
    val version: String,
    val isDebug: Boolean,
    val isDevelopment: Boolean
){
    val type
        get() = if(isDebug) "debug" else "release"

    val isDevelopmentOrDebug = isDevelopment || isDebug

    val versionFlavorDebug
        get() = "$version ${if(isDevelopment) "(Development)" else ""}${if(isDebug) "[Debug]" else ""}"
}