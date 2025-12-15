package com.blockstream.domain.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A use case that performs network operations and caches results in memory.
 * Extends ObservableUseCase to inherit parameter-based reactivity with caching.
 * 
 * Example usage:
 * ```kotlin
 * class GetUserProfileUseCase(
 *     private val userRepository: UserRepository
 * ) : CachedObservableUseCase<GetUserProfileUseCase.Params, UserProfile>() {
 *     
 *     override suspend fun doWork(params: Params) {
 *         val response = userRepository.getUserProfile(params.userId)
 *         if (response is NetworkResponse.Success) {
 *             set(response.data)
 *         }
 *     }
 *     
 *     override fun createObservable(params: Params): Flow<UserProfile> {
 *         return get().filterNotNull()
 *     }
 *     
 *     data class Params(val userId: String)
 * }
 * 
 * // In ViewModel:
 * private val getUserProfile = GetUserProfileUseCase(userRepository)
 * 
 * init {
 *     // Observe cached data that updates when params change
 *     getUserProfile.observe().onEach { profile ->
 *         _uiState.value = UiState.Success(profile)
 *     }.launchIn(viewModelScope)
 *     
 *     // Trigger initial load
 *     getUserProfile(GetUserProfileUseCase.Params("user123"))
 * }
 * 
 * fun refreshProfile() {
 *     // Manually invalidate cache and reload
 *     getUserProfile.invalidate()
 *     getUserProfile(GetUserProfileUseCase.Params("user123"))
 * }
 * ```
 * 
 * Key features:
 * - Caches results in memory across parameter changes
 * - Automatically triggers new network calls when parameters change
 * - Provides synchronous access to cached data via getCurrent()
 * - Supports manual cache invalidation
 */
abstract class CachedObservableUseCase<in P, R> : ObservableUseCase<P, R>() {
    
    private val dataState = MutableStateFlow<R?>(null)

    protected fun set(data: R) {
        dataState.value = data
    }

    fun get(): Flow<R?> {
        return dataState
    }
    
    /**
     * Get current cached value synchronously
     */
    fun getCurrent(): R? = dataState.value
    
    /**
     * Check if data is currently cached
     */
    fun hasCachedData(): Boolean = dataState.value != null
    
    /**
     * Invalidate the cache
     */
    fun invalidate() {
        dataState.value = null
    }
}

/**
 * Legacy alias for backward compatibility
 */
typealias NetworkBoundInMemoryUseCase<P, R> = CachedObservableUseCase<P, R>