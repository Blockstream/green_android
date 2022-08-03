package com.blockstream.green.ui.items

import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren


abstract class AbstractBindingItem<Binding : ViewBinding> : com.mikepenz.fastadapter.binding.AbstractBindingItem<Binding>(){

    var scopeOrNull : CoroutineScope? = null

    val scope: CoroutineScope
        get() {
            if (scopeOrNull == null) {
                scopeOrNull = createScope()
            }
            return scopeOrNull!!
        }


    open fun createScope(): CoroutineScope? = null

    override fun unbindView(binding: Binding) {
        super.unbindView(binding)
        scopeOrNull?.coroutineContext?.cancelChildren()
    }
}