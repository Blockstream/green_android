package com.blockstream.green.lifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

class MergeLiveData<T, K, S>(source1: LiveData<T>, source2: LiveData<K>, private val combine: (data1: T, data2: K) -> S) : MediatorLiveData<S>() {

    private var data1: T? = null
    private var data2: K? = null

    init {
        super.addSource(source1) {
            data1 = it
            combineWhenReady()

        }
        super.addSource(source2) {
            data2 = it
            combineWhenReady()
        }
    }

    private fun combineWhenReady(){
        // combine when both values are non-null
        data1?.let { data1 ->
            data2?.let { data2 ->
                value = combine(data1, data2)
            }
        }
    }

    override fun <T : Any?> addSource(source: LiveData<T>, onChanged: Observer<in T>) {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> removeSource(toRemote: LiveData<T>) {
        throw UnsupportedOperationException()
    }
}