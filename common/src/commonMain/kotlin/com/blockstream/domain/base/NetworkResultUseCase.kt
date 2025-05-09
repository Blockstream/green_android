package com.blockstream.domain.base

import com.blockstream.common.network.NetworkResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class NetworkResultUseCase<in P, R> {
    open operator fun invoke(params: P): Flow<Result<R>> {
        return flow {
            emit(Result.Loading())
            when (val result = doWork(params)) {
                is NetworkResponse.Success -> {
                    emit(Result.Success(result.data))
                }

                is NetworkResponse.Error -> {
                    emit(Result.Error(Throwable(result.message)))
                }
            }
        }
    }

    protected abstract suspend fun doWork(params: P): NetworkResponse<R>
}



