package com.blockstream.domain.base

import com.blockstream.network.NetworkResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A basic use case that performs network operations and returns a result.
 * It emits a loading state, then either a success or error state.
 * 
 * Example usage:
 * ```kotlin
 * class SubmitFormUseCase(
 *     private val apiService: ApiService
 * ) : NetworkResultUseCase<SubmitFormUseCase.Params, FormResponse>() {
 *     
 *     override suspend fun doWork(params: Params): NetworkResponse<FormResponse> {
 *         return apiService.submitForm(
 *             name = params.name,
 *             email = params.email,
 *             message = params.message
 *         )
 *     }
 *     
 *     data class Params(
 *         val name: String,
 *         val email: String,
 *         val message: String
 *     )
 * }
 * 
 * // In ViewModel:
 * private val submitFormUseCase = SubmitFormUseCase(apiService)
 * 
 * fun submitForm(name: String, email: String, message: String) {
 *     submitFormUseCase(SubmitFormUseCase.Params(name, email, message))
 *         .onEach { result ->
 *             when (result) {
 *                 is Result.Loading -> _uiState.value = UiState.Loading
 *                 is Result.Success -> {
 *                     _uiState.value = UiState.Success(result.data)
 *                     // Navigate to success screen
 *                 }
 *                 is Result.Error -> {
 *                     _uiState.value = UiState.Error(result.message)
 *                 }
 *             }
 *         }.launchIn(viewModelScope)
 * }
 * ```
 * 
 * Key features:
 * - One-shot operations that emit loading, then success or error
 * - Automatic error handling and conversion to Result states
 * - Ideal for form submissions, data fetching, or any single network call
 * - No caching or parameter observation
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
                    emit(Result.Error(Throwable(result.message), result.message))
                }
            }
        }
    }

    protected abstract suspend fun doWork(params: P): NetworkResponse<R>
}



