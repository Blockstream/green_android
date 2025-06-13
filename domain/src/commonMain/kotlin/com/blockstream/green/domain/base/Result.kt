package com.blockstream.green.domain.base

sealed interface Result<out T> {
    data class Loading<T>(val data: T? = null) : Result<T>
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable, val message: String) : Result<Nothing>
}
