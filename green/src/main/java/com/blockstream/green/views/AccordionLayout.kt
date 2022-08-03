package com.blockstream.green.views

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Property
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.databinding.ViewDataBinding
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.blockstream.green.R
import mu.KLogging

interface AccordionListener {
    fun expandListener(view: View, position: Int)
    fun arrowClickListener(view: View, position: Int)
    fun copyClickListener(view: View, position: Int)
    fun warningClickListener(view: View, position: Int)
    fun longClickListener(view: View, position: Int)
}

class AccordionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener, View.OnLongClickListener {
    private var expandedIndex = 0
    private var compactSize: Int = 0
    private var expandedSize: Int = 0
    val bindings = mutableMapOf<View, ViewDataBinding>()

    var accordionListener: AccordionListener? = null

    init {
        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.AccordionLayout)

        compactSize = attributes.getDimension(R.styleable.AccordionLayout_compact_size, 0f).toInt()
        expandedSize =
            attributes.getDimension(R.styleable.AccordionLayout_expanded_size, 0f).toInt()

        orientation = VERTICAL

        attributes.recycle()
    }

    fun setExpanded(index: Int, fireListener: Boolean = true, animate: Boolean = true) {
        if(index in 0 until childCount) {
            expandedIndex = index
            getChildAt(expandedIndex)?.also {
                if (fireListener) {
                    accordionListener?.expandListener(it, index)
                }
            }
            expandCards(animate)
        }
    }

    fun getExpanded(): Int {
        return expandedIndex
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        child.setOnClickListener(this)
        child.setOnLongClickListener(this)
        expandCards(false)
    }

    override fun removeAllViews() {
        super.removeAllViews()
        bindings.clear()
        expandedIndex = 0
    }

    override fun removeView(view: View?) {
        super.removeView(view)
        bindings.remove(view)

        if (expandedIndex >= childCount) {
            expandedIndex = childCount - 1
            expandCards(false)
        }
    }

    override fun removeViewAt(index: Int) {
        removeView(getChildAt(index))
    }

    private fun expandCards(animate: Boolean) {
        children.forEachIndexed { index, view ->
            val newHeight = if (index == expandedIndex) {
                expandedSize
            } else {
                compactSize
            }

            val currentHeight = view.layoutParams.height

            if (newHeight != currentHeight) {
                if (animate) {
                    ObjectAnimator.ofInt(view, HeightProperty(), currentHeight, newHeight).apply {
                        this.interpolator = FastOutSlowInInterpolator()
                        this.duration = 200
                        start()
                    }
                } else {
                    view.layoutParams = view.layoutParams.also { it.height = newHeight }
                }
            }
        }
    }

    override fun onClick(v: View) {
        val index = indexOfChild(v)
        if (expandedIndex == index) {
            accordionListener?.arrowClickListener(v, expandedIndex)
        }
        setExpanded(index, true)
    }

    override fun onLongClick(v: View): Boolean {
        accordionListener?.longClickListener(v, indexOfChild(v))
        return true
    }

    fun addBinding(binding: ViewDataBinding) {
        addView(binding.root)
        bindings[binding.root] = binding
    }

    inline fun <reified T : ViewDataBinding> getBinding(index: Int): T? {
        return getBinding(getChildAt(index))
    }

    inline fun <reified T : ViewDataBinding> getBinding(view: View): T? {
        return this.bindings[view] as? T
    }

    companion object : KLogging()
}

class HeightProperty : Property<View, Int>(Int::class.java, "height") {
    override operator fun get(view: View): Int {
        return view.height
    }

    override operator fun set(view: View, value: Int) {
        view.layoutParams = view.layoutParams.also { it.height = value }
    }
}