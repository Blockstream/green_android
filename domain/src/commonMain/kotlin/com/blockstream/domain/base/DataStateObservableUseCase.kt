package com.blockstream.domain.base

import com.blockstream.data.data.DataState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * An [ObservableUseCase] that exposes its work as a [DataState] held in a [StateFlow].
 * The state starts as [DataState.Loading]; subclasses publish results with [set]. Any exception
 * thrown by the work is caught and emitted as [DataState.Error].
 *
 * Subclasses implement [doAsyncWork] instead of [doWork]. Each invocation is serialized by a [Mutex]
 * and runs on [Dispatchers.Default], so calls never overlap and the work stays off the caller thread.
 *
 * Example usage:
 * ```kotlin
 * class GetUserProfileUseCase(
 *     private val userRepository: UserRepository
 * ) : DataStateObservableUseCase<GetUserProfileUseCase.Params, UserProfile>() {
 *
 *     override suspend fun doAsyncWork(params: Params) {
 *         // Throwing here is caught and published as DataState.Error
 *         set(userRepository.getUserProfile(params.userId))
 *     }
 *
 *     override fun createObservable(params: Params): Flow<DataState<UserProfile>> = get()
 *
 *     data class Params(val userId: String)
 * }
 *
 * // In ViewModel:
 * private val getUserProfile = GetUserProfileUseCase(userRepository)
 *
 * init {
 *     // Observe the DataState, which updates as work runs and when params change
 *     getUserProfile.observe().onEach { state ->
 *         _uiState.value = state
 *     }.launchIn(viewModelScope)
 *
 *     // Trigger the load
 *     viewModelScope.launch {
 *         getUserProfile(GetUserProfileUseCase.Params("user123"))
 *     }
 * }
 * ```
 *
 * Key features:
 * - Wraps results in [DataState] (Loading / Success / Error / Empty)
 * - Catches exceptions from [doAsyncWork] and emits them as [DataState.Error]
 * - Serializes invocations with a [Mutex] and runs work on [Dispatchers.Default]
 * - Exposes the current state as a [StateFlow] via [get]
 */
abstract class DataStateObservableUseCase<P, R> : ObservableUseCase<P, DataState<R>>() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val dataState = MutableStateFlow<DataState<R>>(DataState.Loading)

    private val mutex = Mutex()

    protected abstract suspend fun doAsyncWork(params: P)

    /**
     * Gate that decides whether [doAsyncWork] runs for the given [params]. When it returns false the
     * invocation is skipped and the current state is left unchanged. Defaults to always running;
     * override to skip work under conditions such as a disconnected session.
     */
    protected open fun shouldExecute(params: P): Boolean = true

    final override suspend fun doWork(params: P) = mutex.withLock {
        if (!shouldExecute(params)) return@withLock
        withContext(context = Dispatchers.Default) {
            try {
                doAsyncWork(params)
            } catch (e: Exception) {
                e.printStackTrace()
                set(DataState.Error(e))
            }
        }
    }

    protected fun set(data: R) {
        dataState.value = DataState.Success(data)
    }

    protected fun set(data: DataState<R>) {
        dataState.value = data
    }

    open fun get(): StateFlow<DataState<R>> {
        return dataState.asStateFlow()
    }

    /**
     * Get current cached value synchronously
     */
    fun getCurrent(): DataState<R> = dataState.value

    // Share the stream so multiple collectors drive a single createObservable execution
    private val sharedObservable: Flow<DataState<R>> by lazy {
        super.observe().shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)
    }

    override fun observe(): Flow<DataState<R>> = sharedObservable
}

/**
 * Invokes the use case with [params], then suspends until the first settled [DataState] — the first
 * non-null emission that is not [DataState.Loading] (i.e. [DataState.Success], [DataState.Error] or
 * [DataState.Empty]).
 */
suspend fun <P, T> DataStateObservableUseCase<P, T>.firstSettled(params: P): DataState<T> {
    invoke(params)
    // The shared observer may still replay the value from before this invocation.
    return get().first { !it.isLoading() }
}

/**
 * [firstSettled] for parameterless use cases — invokes with [Unit].
 */
suspend fun <T> DataStateObservableUseCase<Unit, T>.firstSettled(): DataState<T> =
    firstSettled(Unit)

