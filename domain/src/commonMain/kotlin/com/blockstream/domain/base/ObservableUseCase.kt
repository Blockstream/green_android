package com.blockstream.domain.base

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

/**
 * A base class for use cases that need to perform network operations and return a Flow of results.
 * It uses a MutableStateFlow to hold the parameters and emits the results based on the parameters.
 * It calls the `doWork` every time the parameters change.
 * 
 * Example usage:
 * ```kotlin
 * class SearchProductsUseCase(
 *     private val productRepository: ProductRepository
 * ) : ObservableUseCase<SearchProductsUseCase.Params, List<Product>>() {
 *     
 *     override suspend fun doWork(params: Params) {
 *         // This is called every time invoke() is called with new params
 *         // You might update a local database or trigger side effects here
 *     }
 *     
 *     override fun createObservable(params: Params): Flow<List<Product>> {
 *         // Return a Flow that emits data based on the parameters
 *         return productRepository.searchProducts(
 *             query = params.query,
 *             category = params.category
 *         )
 *     }
 *     
 *     data class Params(
 *         val query: String,
 *         val category: String? = null
 *     )
 * }
 * 
 * // In ViewModel:
 * private val searchProductsUseCase = SearchProductsUseCase(productRepository)
 * 
 * init {
 *     // Observe search results - will update whenever parameters change
 *     searchProductsUseCase.observe()
 *         .onEach { products ->
 *             _searchResults.value = products
 *         }
 *         .launchIn(viewModelScope)
 * }
 * 
 * fun onSearchQueryChanged(query: String) {
 *     // Trigger new search - observe() will emit new results
 *     viewModelScope.launch {
 *         searchProductsUseCase(SearchProductsUseCase.Params(query))
 *     }
 * }
 * ```
 * 
 * Key features:
 * - Reactive to parameter changes - new params trigger new emissions
 * - Separates side effects (doWork) from data observation (createObservable)
 * - Ideal for search functionality, filters, or any parameter-driven queries
 * - No built-in caching (use CachedObservableUseCase if you need caching)
 */
abstract class ObservableUseCase<in P, R> {
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



