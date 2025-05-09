package com.blockstream.domain.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest


abstract class NetworkBoundUseCase<in P, R> {
    private val paramState = MutableStateFlow<P?>(null)

    open suspend operator fun invoke(params: P) {
        paramState.value = params
        doWork(params)
    }

    protected abstract suspend fun doWork(params: P)
    protected abstract fun createObservable(params: P): Flow<R>

    fun observe() = paramState.filterNotNull().flatMapLatest { createObservable(it) }

}



