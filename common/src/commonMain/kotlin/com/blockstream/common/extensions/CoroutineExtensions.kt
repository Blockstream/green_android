package com.blockstream.common.extensions

import com.blockstream.common.CountlyBase
import kotlinx.coroutines.CoroutineExceptionHandler


// Log and handle the exception. Prevent unhanded exception crash
fun logException(
    countly: CountlyBase
): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, exception ->
//        if (isDevelopmentOrDebug) {
            exception.printStackTrace()
//        }

        countly.recordException(exception)
    }
}