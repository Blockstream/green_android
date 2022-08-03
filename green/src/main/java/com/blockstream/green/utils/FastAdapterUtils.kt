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

fun <Model, Item : GenericItem> ModelAdapter<Model, Item>.observeMap(
    lifecycleOwner: LifecycleOwner, liveData: LiveData<Map<*, *>>,
    toModel: (Map.Entry<*, *>) -> Model,
    observer: ((Map<Model, Item>) -> Unit)? = null,
): ModelAdapter<Model, Item> {
    liveData.observe(lifecycleOwner) {
        val list = it.map {
            toModel(it)
        }

        FastAdapterDiffUtil.set(this, intercept(list), true)

        observer?.invoke(it as Map<Model, Item>)
    }
    return this
}

fun <Model, Item : GenericItem> ModelAdapter<Model, Item>.observeMap(
    scope: CoroutineScope,
    flow: Flow<Map<*, *>>,
    toModel: (Map.Entry<*, *>) -> Model,
    observer: ((Map<Model, Item>) -> Unit)? = null,
): ModelAdapter<Model, Item> {
    flow.onEach {
        val list = it.map {
            toModel(it)
        }
        FastAdapterDiffUtil.set(this, intercept(list), true)
        observer?.invoke(it as Map<Model, Item>)
    }.launchIn(scope)
    return this
}