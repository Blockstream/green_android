package com.blockstream.green.domain.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Similar to NetworkBoundUseCase, but this one is used for in-memory data.
 */
abstract class NetworkBoundInMemoryUseCase<in P, R> : NetworkBoundUseCase<P, R>() {
    private val dataState = MutableStateFlow<R?>(null)

    fun set(data: R) {
        dataState.value = data
    }

    fun get(): Flow<R?> {
        return dataState
    }

}



