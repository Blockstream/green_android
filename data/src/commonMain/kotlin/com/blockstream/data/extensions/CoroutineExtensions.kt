package com.blockstream.data.extensions

import com.blockstream.data.CountlyBase
import com.blockstream.data.config.AppInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatformTools
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// Log and handle the exception. Prevent unhanded exception crash
suspend fun <T> tryCatch(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): T? =
    withContext(context = context + logException()) {
        try {
            block()
        } catch (e: CancellationException) {
            // Re-throw cancellation to preserve coroutine cancellation behavior
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            KoinPlatformTools.defaultContext().get().getOrNull<CountlyBase>()?.also {
                it.recordException(e)
            }
            null
        }
    }

// Handle exceptions
fun CoroutineScope.launchSafe(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch(context = context, start = start) {
        try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Handle exceptions
fun <T> CoroutineScope.asyncSafe(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T?> {
    return async(context = context, start = start) {
        try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

public inline fun <T, R> T.letTryCatch(block: (T) -> R): R? {
    return try {
        block(this)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun <T> tryCatchNull(block: () -> T): T? = try {
    block()
} catch (e: Exception) {
    e.printStackTrace()

    KoinPlatformTools.defaultContext().get().getOrNull<CountlyBase>()?.also {
        it.recordException(e)
    }
    null
}

suspend fun <T> tryCatchNullSuspend(block: suspend () -> T): T? = try {
    block()
} catch (e: Exception) {
    e.printStackTrace()

    KoinPlatformTools.defaultContext().get().getOrNull<CountlyBase>()?.also {
        it.recordException(e)
    }
    null
}

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

fun logException(): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, exception ->
        if (KoinPlatformTools.defaultContext().get().get<AppInfo>().isDevelopmentOrDebug) {
            exception.printStackTrace()
        }

        KoinPlatformTools.defaultContext().get().getOrNull<CountlyBase>()?.also {
            it.recordException(exception)
        }
    }
}

fun handleException(): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, exception ->
        if (KoinPlatformTools.defaultContext().get().get<AppInfo>().isDevelopmentOrDebug) {
            exception.printStackTrace()
        }
    }
}

fun MutableStateFlow<Boolean>.toggle() {
    this.value = !value
}

fun CoroutineScope.supervisorJob() = CoroutineScope(context = coroutineContext + SupervisorJob() + Dispatchers.IO + handleException())

fun CoroutineScope.cancelChildren(
    cause: CancellationException? = null
) = coroutineContext[Job]?.cancelChildren(cause)