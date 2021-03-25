package com.blockstream.green.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil

/**
Helper function to set data from LiveData
 */
fun <Model, Item : GenericItem> ModelAdapter<Model, Item>.observe(
    lifecycleOwner: LifecycleOwner, liveData: LiveData<List<Model>>
): ModelAdapter<Model, Item> {
    liveData.observe(lifecycleOwner) {
        FastAdapterDiffUtil.set(this, intercept(it), true)
    }
    return this
}