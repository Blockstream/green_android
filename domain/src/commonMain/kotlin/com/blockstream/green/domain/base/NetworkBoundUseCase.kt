package com.blockstream.green.domain.base

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

/**
 * A base class for use cases that need to perform network operations and return a Flow of results.
 * It uses a MutableStateFlow to hold the parameters and emits the results based on the parameters.
 * It calls the `doWork` every time the parameters change.
 */
abstract class NetworkBoundUseCase<in P, R> {
    private val paramState = MutableStateFlow<P?>(null)

    open suspend operator fun invoke(params: P) {
        paramState.value = params
        doWork(params)
    }

    protected abstract suspend fun doWork(params: P)
    protected abstract fun createObservable(params: P): Flow<R>

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe() = paramState.filterNotNull().flatMapLatest { createObservable(it) }

}



