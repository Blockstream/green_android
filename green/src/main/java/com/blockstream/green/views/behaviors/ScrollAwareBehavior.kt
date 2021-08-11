package com.blockstream.green.views.behaviors

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min


class ScrollAwareBehavior(context: Context, attributeSet: AttributeSet) :
    CoordinatorLayout.Behavior<View>(
        context,
        attributeSet
    ) {

    private var isEnabled: Boolean = true
    private var mRestoreAnimation: ObjectAnimator? = null

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        return dependency is RecyclerView
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return true
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        type: Int
    ) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)

        if (isEnabled) restore(child)
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        super.onNestedScroll(
            coordinatorLayout,
            child,
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            consumed
        )

        if (!isEnabled) return

        val params = child.layoutParams as CoordinatorLayout.LayoutParams

        val verticalGravity = params.gravity and Gravity.VERTICAL_GRAVITY_MASK

        val factor = 1

        if (verticalGravity == Gravity.BOTTOM) {
            val maxY = child.height + params.bottomMargin.toFloat()
            val newY = child.translationY + dyConsumed / factor

            child.translationY = max(0f, min(newY, maxY))
        } else { // if(verticalGravity == Gravity.TOP){
            val maxY = -(child.height + params.topMargin.toFloat())
            val newY = (child.translationY - dyConsumed / factor)

            child.translationY = min(0f, max(newY, maxY))
        }

        cancelAnimation()
    }


    private fun restore(child: View?) {
        if (mRestoreAnimation == null) {
            mRestoreAnimation = ObjectAnimator.ofFloat(child, "translationY", 0f).apply {
                duration = 150
                startDelay = 250

                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationEnd(animation: Animator) {
                        mRestoreAnimation = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        mRestoreAnimation = null
                    }

                    override fun onAnimationRepeat(animation: Animator) {}
                })
                start()
            }
        }
    }

    fun cancelAnimation(){
        mRestoreAnimation = mRestoreAnimation?.let {
            it.cancel()
            null
        }
    }

    fun disable(child: View?, restore: Boolean) {
        isEnabled = false
        cancelAnimation()

        if(restore) {
            restore(child)
        }
    }

    fun enable() {
        isEnabled = true
    }
}