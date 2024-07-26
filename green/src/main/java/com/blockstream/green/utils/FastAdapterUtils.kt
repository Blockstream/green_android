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