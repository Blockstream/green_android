package com.blockstream.green.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View

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