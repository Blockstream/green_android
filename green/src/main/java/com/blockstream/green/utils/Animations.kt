package com.blockstream.green.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.widget.ViewAnimator

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
    ObjectAnimator.ofFloat(this, "translationY", 0.0f, 30.0f, 0.0f, ).apply {
        duration = 500
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        start()
    }
}

//fun View.slideRight(translationX: Float, animationEnd: () -> Unit) {
//    ObjectAnimator.ofFloat(this, "translationX", translationX).apply {
//        duration = 200
//
//        addListener(object : Animator.AnimatorListener {
//            override fun onAnimationStart(animation: Animator) {}
//            override fun onAnimationEnd(animation: Animator) {
//                animationEnd.invoke()
//            }
//
//            override fun onAnimationCancel(animation: Animator) {}
//            override fun onAnimationRepeat(animation: Animator) {}
//        })
//        start()
//    }
//}