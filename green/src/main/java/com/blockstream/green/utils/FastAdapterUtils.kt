package com.blockstream.green.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
Helper function to set data from LiveData
 */
fun <Model, Item : GenericItem> ModelAdapter<Model, Item>.observeList(
    lifecycleOwner: LifecycleOwner,
    liveData: LiveData<List<Model>>,
    useDiffUtil: Boolean = true,
    onEvent: ((List<Model>) -> Unit)? = null,
): ModelAdapter<Model, Item> {
    liveData.observe(lifecycleOwner) {
        onEvent?.invoke(it)
        if(useDiffUtil) {
            FastAdapterDiffUtil.set(this, intercept(it), true)
        }else{
            set(it)
        }
    }
    return this
}

fun <Model, Item : GenericItem> ModelAdapter<Model, Item>.observeList(
    scope: CoroutineScope,
    flow: Flow<List<Model>>,
    onEvent: ((List<Model>) -> Unit)? = null
): ModelAdapter<Model, Item> {
    flow.onEach {
        onEvent?.invoke(it)
        FastAdapterDiffUtil.set(this, intercept(it), true)
    }.launchIn(scope)
    return this
}

fun <Model, Item : GenericItem, T> ModelAdapter<Model, Item>.observeLiveData(
    lifecycleOwner: LifecycleOwner, liveData: LiveData<T>,
    toList: (T) -> List<Model>,
    observer: ((T) -> Unit)? = null,
): ModelAdapter<Model, Item> {
    liveData.observe(lifecycleOwner) {
        FastAdapterDiffUtil.set(this, intercept(toList(it)), true)
        observer?.invoke(it)
    }
    return this
}

fun <Model, Item : GenericItem, T> ModelAdapter<Model, Item>.observeFlow(
    scope: CoroutineScope,
    flow: Flow<T>,
    useDiffUtil: Boolean = true,
    toList: (T) -> List<Model>,
    observer: ((T) -> Unit)? = null,
): ModelAdapter<Model, Item> {
    flow.onEach {
        toList(it).also {
            if(useDiffUtil) {
                FastAdapterDiffUtil.set(this, intercept(it), true)
            }else{
                set(it)
            }
        }
        observer?.invoke(it)

    }.launchIn(scope)
    return this
}