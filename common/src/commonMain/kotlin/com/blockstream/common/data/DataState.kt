package com.blockstream.common.data

import kotlinx.coroutines.flow.StateFlow

/**
 * DataState class is a wrapper class to wrap th data between the app layers.
 *
 * @author Mohammedsaif Kordia
 */
sealed class DataState<out T> {

    /**
     * A generic data class to wrap the succeeded data result.
     *
     * @param data the result data
     */
    data class Success<out T>(val data: T) : DataState<T>()

    /**
     * A data class to wrap the fail exception result.
     *
     * @param exception the exception result
     */
    data class Error(val exception: Exception) : DataState<Nothing>()

    /**
     * A Nothing object that emits the loading state.
     */
    data object Loading : DataState<Nothing>()

    /**
     * A Nothing object that emits the empty result state,
     * we use this if the request or the query succeeded but with empty results.
     */
    data object Empty : DataState<Nothing>()

    fun data() = (this as? Success)?.data

    fun isSuccess() = this is Success

    fun isLoading() = this is Loading

    fun isEmpty(): Boolean {
        return this is Empty || ((this as? Success<*>)?.data as? List<*>)?.isEmpty() == true
    }

    fun isNotEmpty(): Boolean {
        return (if (this is Success<*>) {
            (this.data as? List<*>)?.isNotEmpty() ?: true
        } else {
            false
        })
    }

    inline fun <R> mapSuccess(transform: (T) -> R): DataState<R> {
        return when(this){
            is Error -> this
            is Loading -> this
            is Empty -> this
            is Success -> {
                Success(transform(data))
            }
        }
    }

    companion object{
        fun <T> successOrEmpty(value: T?): DataState<T> {
            return if(value == null){
                Empty
            }else{
                Success(value)
            }
        }
    }
}

fun <T> StateFlow<DataState<T>>.data() : T? {
    return this.value.data()
}

fun <T> StateFlow<DataState<T>>.isEmpty() : Boolean {
    return this.value.isEmpty()
}