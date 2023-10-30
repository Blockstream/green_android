package com.blockstream.common.extensions

import com.blockstream.common.CountlyBase
import com.blockstream.common.data.AppInfo
import kotlinx.coroutines.CoroutineExceptionHandler
import org.koin.mp.KoinPlatformTools


// Log and handle the exception. Prevent unhanded exception crash
fun logException(
    countly: CountlyBase
): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, exception ->
        if (KoinPlatformTools.defaultContext().get().get<AppInfo>().isDevelopmentOrDebug) {
            exception.printStackTrace()
        }

        countly.recordException(exception)
    }
}