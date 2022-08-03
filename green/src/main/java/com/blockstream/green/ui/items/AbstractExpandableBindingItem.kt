package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import com.blockstream.green.extensions.context
import com.mikepenz.fastadapter.IItemVHFactory
import com.mikepenz.fastadapter.binding.BindingViewHolder
import com.mikepenz.fastadapter.expandable.items.AbstractExpandableItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren

abstract class AbstractExpandableBindingItem<Binding : ViewBinding> : AbstractExpandableItem<BindingViewHolder<Binding>>(), IItemVHFactory<BindingViewHolder<Binding>> {

    override val layoutRes: Int = 0

    var scopeOrNull : CoroutineScope? = null
    val scope: CoroutineScope
        get() {
            if (scopeOrNull == null) {
                scopeOrNull = createScope()
            }
            return scopeOrNull!!
        }

    open fun createScope(): CoroutineScope? = null

    fun context(binding: Binding) = binding.context()

    @CallSuper
    override fun bindView(holder: BindingViewHolder<Binding>, payloads: List<Any>) {
        super.bindView(holder, payloads)
        bindView(holder.binding, payloads)
    }

    open fun bindView(binding: Binding, payloads: List<Any>) {}

    override fun unbindView(holder: BindingViewHolder<Binding>) {
        super.unbindView(holder)
        unbindView(holder.binding)
        scopeOrNull?.coroutineContext?.cancelChildren()
    }

    open fun unbindView(binding: Binding) {}

    override fun attachToWindow(holder: BindingViewHolder<Binding>) {
        super.attachToWindow(holder)
        attachToWindow(holder.binding)
    }

    open fun attachToWindow(binding: Binding) {}

    override fun detachFromWindow(holder: BindingViewHolder<Binding>) {
        super.detachFromWindow(holder)
        detachFromWindow(holder.binding)
    }

    open fun detachFromWindow(binding: Binding) {}

    /**
     * This method is called by generateView(Context ctx), generateView(Context ctx, ViewGroup parent) and getViewHolder(ViewGroup parent)
     * it will generate the ViewBinding. You have to provide the correct binding class.
     */
    abstract fun createBinding(inflater: LayoutInflater, parent: ViewGroup? = null): Binding

    /** Generates a ViewHolder from this Item with the given parent */
    override fun getViewHolder(parent: ViewGroup): BindingViewHolder<Binding> {
        return getViewHolder(createBinding(LayoutInflater.from(parent.context), parent))
    }

    /** Generates a ViewHolder from this Item with the given ViewBinding */
    open fun getViewHolder(viewBinding: Binding): BindingViewHolder<Binding> {
        return BindingViewHolder(viewBinding)
    }

    override fun getViewHolder(v: View): BindingViewHolder<Binding> {
        TODO("Not yet implemented")
    }


}