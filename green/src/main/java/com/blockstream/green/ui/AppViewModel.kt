package com.blockstream.green.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.blockstream.green.utils.ConsumableEvent
import io.reactivex.rxjava3.disposables.CompositeDisposable

open class AppViewModel : ViewModel(){
    internal val disposables = CompositeDisposable()

    val onEvent = MutableLiveData<ConsumableEvent<Any>>()
    val onProgress = MutableLiveData(false)
    val onError = MutableLiveData<ConsumableEvent<Throwable>>()

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}