package com.blockstream.common.extensions

import com.blockstream.common.CountlyBase
import com.blockstream.green.data.config.AppInfo
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatformTools
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// Log and handle the exception. Prevent unhanded exception crash
suspend fun <T> tryCatch(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): T? =
    withContext(context = context + logException()) {
        block()
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

fun <T> Flow<T>.launchIn(viewModel: ViewModel) = launchIn(viewModel.viewModelScope.coroutineScope)

fun CoroutineScope.supervisorJob() = CoroutineScope(context = coroutineContext + SupervisorJob() + Dispatchers.IO + handleException())

fun CoroutineScope.cancelChildren(
    cause: CancellationException? = null
) = coroutineContext[Job]?.cancelChildren(cause)
