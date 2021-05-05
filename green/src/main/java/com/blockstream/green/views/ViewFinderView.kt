package com.blockstream.green.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px

class ViewFinderView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private val mMaskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mFramePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mFramePath: Path
    private var frameRect: Rect? = null
    private var mFrameCornersSize = 0
    private var mFrameRatioWidth = 1f
    private var mFrameRatioHeight = 1f
    private var mFrameSize = 0.75f

    constructor(context: Context) : this(context, null) {}

    override fun onDraw(canvas: Canvas) {
        val frameRect = frameRect ?: return
        val width = width
        val height = height
        val top = frameRect.top
        val left = frameRect.left
        val right = frameRect.right
        val bottom = frameRect.bottom
        canvas.drawRect(0f, 0f, width.toFloat(), top.toFloat(), mMaskPaint)
        canvas.drawRect(0f, top.toFloat(), left.toFloat(), bottom.toFloat(), mMaskPaint)
        canvas.drawRect(
            right.toFloat(),
            top.toFloat(),
            width.toFloat(),
            bottom.toFloat(),
            mMaskPaint
        )
        canvas.drawRect(0f, bottom.toFloat(), width.toFloat(), height.toFloat(), mMaskPaint)
        mFramePath.reset()
        mFramePath.moveTo(left.toFloat(), (top + mFrameCornersSize).toFloat())
        mFramePath.lineTo(left.toFloat(), top.toFloat())
        mFramePath.lineTo((left + mFrameCornersSize).toFloat(), top.toFloat())
        mFramePath.moveTo((right - mFrameCornersSize).toFloat(), top.toFloat())
        mFramePath.lineTo(right.toFloat(), top.toFloat())
        mFramePath.lineTo(right.toFloat(), (top + mFrameCornersSize).toFloat())
        mFramePath.moveTo(right.toFloat(), (bottom - mFrameCornersSize).toFloat())
        mFramePath.lineTo(right.toFloat(), bottom.toFloat())
        mFramePath.lineTo((right - mFrameCornersSize).toFloat(), bottom.toFloat())
        mFramePath.moveTo((left + mFrameCornersSize).toFloat(), bottom.toFloat())
        mFramePath.lineTo(left.toFloat(), bottom.toFloat())
        mFramePath.lineTo(left.toFloat(), (bottom - mFrameCornersSize).toFloat())
        canvas.drawPath(mFramePath, mFramePaint)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        invalidateFrameRect(right - left, bottom - top)
    }

    fun setFrameAspectRatio(
        @FloatRange(from =  0.0, fromInclusive = false) ratioWidth: Float,
        @FloatRange(from =  0.0, fromInclusive = false) ratioHeight: Float
    ) {
        mFrameRatioWidth = ratioWidth
        mFrameRatioHeight = ratioHeight
        invalidateFrameRect()
        if (isLaidOut(this)) {
            invalidate()
        }
    }

    @get:FloatRange(from = 0.0, fromInclusive = false)
    var frameAspectRatioWidth: Float
        get() = mFrameRatioWidth
        set(ratioWidth) {
            mFrameRatioWidth = ratioWidth
            invalidateFrameRect()
            if (isLaidOut(this)) {
                invalidate()
            }
        }

    @get:FloatRange(from =  0.0, fromInclusive = false)
    var frameAspectRatioHeight: Float
        get() = mFrameRatioHeight
        set(ratioHeight) {
            mFrameRatioHeight = ratioHeight
            invalidateFrameRect()
            if (isLaidOut(this)) {
                invalidate()
            }
        }

    @get:ColorInt
    var maskColor: Int
        get() = mMaskPaint.color
        set(color) {
            mMaskPaint.color = color
            if (isLaidOut(this)) {
                invalidate()
            }
        }

    @get:ColorInt
    var frameColor: Int
        get() = mFramePaint.color
        set(color) {
            mFramePaint.color = color
            if (isLaidOut(this)) {
                invalidate()
            }
        }

    @get:Px
    var frameThickness: Int
        get() = mFramePaint.strokeWidth.toInt()
        set(thickness) {
            mFramePaint.strokeWidth = thickness.toFloat()
            if (isLaidOut(this)) {
                invalidate()
            }
        }

    @get:Px
    var frameCornersSize: Int
        get() = mFrameCornersSize
        set(size) {
            mFrameCornersSize = size
            if (isLaidOut(this)) {
                invalidate()
            }
        }

    @get:FloatRange(from = 0.1, to = 1.0)
    var frameSize: Float
        get() = mFrameSize
        set(size) {
            mFrameSize = size
            invalidateFrameRect()
            if (isLaidOut(this)) {
                invalidate()
            }
        }

    private fun invalidateFrameRect(width: Int = getWidth(), height: Int = getHeight()) {
        if (width > 0 && height > 0) {
            val viewAR = width.toFloat() / height.toFloat()
            val frameAR = mFrameRatioWidth / mFrameRatioHeight
            val frameWidth: Int
            val frameHeight: Int
            if (viewAR <= frameAR) {
                frameWidth = Math.round(width * mFrameSize)
                frameHeight = Math.round(frameWidth / frameAR)
            } else {
                frameHeight = Math.round(height * mFrameSize)
                frameWidth = Math.round(frameHeight * frameAR)
            }
            val frameLeft = (width - frameWidth) / 2
            val frameTop = (height - frameHeight) / 2
            frameRect = Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight)
        }
    }

    internal inner class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int
            get() = right - left
        val height: Int
            get() = bottom - top

        override fun hashCode(): Int {
            return 31 * (31 * (31 * left + top) + right) + bottom
        }

        override fun equals(other: Any?): Boolean {
            return when {
                other === this -> {
                    true
                }
                other is Rect -> {
                    val other = other
                    left == other.left && top == other.top && right == other.right && bottom == other.bottom
                }
                else -> {
                    false
                }
            }
        }

        override fun toString(): String {
            return "[(" + left + "; " + top + ") - (" + right + "; " + bottom + ")]"
        }
    }

    companion object {
        fun isLaidOut(view: View): Boolean {
            return view.isLaidOut
        }
    }

    init {
        mFramePaint.style = Paint.Style.STROKE
        mFramePath = Path()
    }
}
