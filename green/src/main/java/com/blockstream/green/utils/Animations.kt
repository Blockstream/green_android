package com.blockstream.green.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View


fun View.fadeIn(duration: Long = 1000, skipIfAnimated: Boolean = true) {
    if(skipIfAnimated && this.alpha == 1.0f) return

    ObjectAnimator.ofFloat(
        this,
        "alpha",
        0f,
        1f
    ).apply {
        this.duration = duration
        this.start()
    }
}

fun View.fadeOut(duration: Long = 1000, skipIfAnimated: Boolean = true) {
    if(skipIfAnimated && this.alpha == 0.0f) return

    ObjectAnimator.ofFloat(
        this,
        "alpha",
        1f,
        0f
    ).apply {
        this.duration = duration
        this.start()
    }
}

fun View.pulse(repeat: Boolean = false) {
    AnimatorSet().also {
        it.playTogether(
            listOf(ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.05f, 1f),
            ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.05f, 1f)).onEach { obj ->
                if(repeat){
                    obj.repeatMode = ValueAnimator.RESTART
                    obj.repeatCount = ValueAnimator.INFINITE
                }
            },
        )
        it.duration = 400
        it.start()
    }
}

fun View.bounceDown(){
    ObjectAnimator.ofFloat(this, "translationY", 0.0f, 30.0f, 0.0f).apply {
        duration = 500
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        start()
    }
}
