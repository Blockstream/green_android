package com.blockstream.green.extensions

import androidx.lifecycle.MutableLiveData
import com.blockstream.green.data.Countly
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.isDevelopmentOrDebug
import kotlinx.coroutines.CoroutineExceptionHandler


// Log and handle the exception. Prevent unhanded exception crash
fun logException(
    countly: Countly,
    onError: MutableLiveData<ConsumableEvent<Throwable>>? = null
): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, exception ->
        if (isDevelopmentOrDebug) {
            exception.printStackTrace()
        }

        countly.recordException(exception)

        // Show error
        onError?.postValue(ConsumableEvent(exception))
    }
}

fun handleException(
    onError: MutableLiveData<ConsumableEvent<Throwable>>? = null
): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, exception ->
        if (isDevelopmentOrDebug) {
            exception.printStackTrace()
        }

        // Show error
        onError?.postValue(ConsumableEvent(exception))
    }
}