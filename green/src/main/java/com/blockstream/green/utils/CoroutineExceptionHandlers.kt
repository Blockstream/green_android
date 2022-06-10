package com.blockstream.green.utils

import com.blockstream.green.data.Countly
import kotlinx.coroutines.CoroutineExceptionHandler


// Log and handle the exception. Prevent unhanded exception crash
fun logException(countly: Countly): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, exception ->
        if (isDevelopmentOrDebug) {
            exception.printStackTrace()
        }

        countly.recordException(exception)
    }
}