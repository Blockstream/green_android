package com.blockstream.green.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart

fun View.shake() {
    ObjectAnimator.ofFloat(
        this,
        "translationX",
        0f,
        40f,
        -40f,
        30f,
        -30f,
        25f,
        -25f,
        15f,
        -15f,
        6f,
        -6f,
        0f
    ).apply {
        duration = 1000
        start()
    }
}

fun View.pulse() {
    AnimatorSet().also {
        it.playTogether(
            ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.05f, 1f),
            ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.05f, 1f),
        )
        it.duration = 400
        it.start()
    }
}

fun View.rotate() {
    ObjectAnimator.ofFloat(this, "rotation", -360f, 0f).apply {
        duration = 600
        start()
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

fun View.starWarsAndHide(offset: Int = this.height, duration: Long = 500) {
    val tag = 57384359 // random animation tag

    // Cancel previous animation
    this.getTag(tag)?.let {
        if (it is Animator) {
            it.cancel()
        }
    }

    val showAnimation = AnimatorSet().also {
        it.playTogether(
            ObjectAnimator.ofFloat(this, "alpha", 0.0f, 1.0f),
            ObjectAnimator.ofFloat(this, "scaleY", 1.5f, 1f),
            ObjectAnimator.ofFloat(this, "scaleX", 1.5f, 1f),
            ObjectAnimator.ofFloat(this, "translationY", 0.0f, -offset.toFloat())
        )
        it.duration = 300
    }

    val hideAnimation = AnimatorSet().also {
        it.startDelay = duration

        it.playTogether(
            ObjectAnimator.ofFloat(this, "alpha", 1f, 0.0f),
            ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.5f),
            ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.5f),
            ObjectAnimator.ofFloat(this, "translationY", -offset.toFloat(), 0.0f)
        )

        it.duration = 500
    }

    AnimatorSet().also { it ->
        it.doOnStart {
            this.setTag(tag, it)
        }
        it.doOnEnd {
            this.setTag(tag, null)
        }
        it.playSequentially(showAnimation, hideAnimation)
        it.start()
    }
}