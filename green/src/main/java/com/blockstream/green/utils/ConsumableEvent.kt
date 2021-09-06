package com.blockstream.green.utils

open class ConsumableEvent<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again
     * If filter is provided handles only when filter returns true.
     */
    fun getContentIfNotHandledOrReturnNull(filter: ((content: T) -> Boolean)? = null): T? {
        return if (hasBeenHandled) {
            null
        } else {
            if(filter == null || filter.invoke(content)) {
                hasBeenHandled = true
                content
            }else{
                null
            }
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}
