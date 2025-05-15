package com.blockstream.green.domain.base

import com.blockstream.green.network.NetworkResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A basic use cases that perform network operations and return a result.
 * It emits a loading state, then either a success or error state.
 */
abstract class NetworkResultUseCase<in P, R> {
    open operator fun invoke(params: P): Flow<Result<R>> {
        return flow {
            emit(Result.Loading())
            val result = try {
                doWork(params)
            } catch (e: Exception) {
                NetworkResponse.Error(code = 500, message = e.message ?: "Unknown error")
            }

            when (result) {
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



