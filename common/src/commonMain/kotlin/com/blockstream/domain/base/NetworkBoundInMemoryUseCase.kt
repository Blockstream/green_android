package com.blockstream.domain.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


abstract class NetworkBoundInMemoryUseCase<in P, R> : NetworkBoundUseCase<P, R>() {
    private val dataState = MutableStateFlow<R?>(null)

    fun set(data: R) {
        dataState.value = data
    }

    fun get(): Flow<R?> {
        return dataState
    }

}



