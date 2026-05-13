package com.blockstream.compose.android.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px

class ViewFinderView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private val mMaskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mFramePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val mEraserPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val mArcRect = RectF()
    private val mFramePath: Path = Path()

    private var frameRect: Rect? = null
    private var mFrameCornersSize = 0
    private var mFrameRatioWidth = 1f
    private var mFrameRatioHeight = 1f
    private var mFrameSize = 0.75f
    private var mFrameCornersRadius = 50f

    constructor(context: Context) : this(context, null)

    override fun onDraw(canvas: Canvas) {
        val frameRect = frameRect ?: return
        val width = width.toFloat()
        val height = height.toFloat()
        val top = frameRect.top.toFloat()
        val left = frameRect.left.toFloat()
        val right = frameRect.right.toFloat()
        val bottom = frameRect.bottom.toFloat()
        val r = mFrameCornersRadius

        val saveCount = canvas.saveLayer(0f, 0f, width, height, null)
        canvas.drawRect(0f, 0f, width, height, mMaskPaint)

        canvas.drawRoundRect(left, top, right, bottom, r, r, mEraserPaint)
        canvas.restoreToCount(saveCount)

        mFramePath.reset()
        val cs = mFrameCornersSize.toFloat()

        // Top-left
        mFramePath.moveTo(left, top + cs)
        mFramePath.lineTo(left, top + r)
        mArcRect.set(left, top, left + r * 2, top + r * 2)
        mFramePath.arcTo(mArcRect, 180f, 90f)
        mFramePath.lineTo(left + cs, top)

        // Top-right
        mFramePath.moveTo(right - cs, top)
        mFramePath.lineTo(right - r, top)
        mArcRect.set(right - r * 2, top, right, top + r * 2)
        mFramePath.arcTo(mArcRect, 270f, 90f)
        mFramePath.lineTo(right, top + cs)

        // Bottom-right
        mFramePath.moveTo(right, bottom - cs)
        mFramePath.lineTo(right, bottom - r)
        mArcRect.set(right - r * 2, bottom - r * 2, right, bottom)
        mFramePath.arcTo(mArcRect, 0f, 90f)
        mFramePath.lineTo(right - cs, bottom)

        // Bottom-left
        mFramePath.moveTo(left + cs, bottom)
        mFramePath.lineTo(left + r, bottom)
        mArcRect.set(left, bottom - r * 2, left + r * 2, bottom)
        mFramePath.arcTo(mArcRect, 90f, 90f)
        mFramePath.lineTo(left, bottom - cs)

        canvas.drawPath(mFramePath, mFramePaint)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        invalidateFrameRect(right - left, bottom - top)
    }

    fun setFrameAspectRatio(
        @FloatRange(from = 0.0, fromInclusive = false)
        ratioWidth: Float,
        @FloatRange(from = 0.0, fromInclusive = false)
        ratioHeight: Float
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

    @get:FloatRange(from = 0.0, fromInclusive = false)
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

    var frameCornersRadius: Float
        get() = mFrameCornersRadius
        set(value) {
            mFrameCornersRadius = value
            if (isLaidOut(this)) invalidate()
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
                    left == other.left && top == other.top && right == other.right && bottom == other.bottom
                }

                else -> {
                    false
                }
            }
        }

        override fun toString(): String {
            return "[($left; $top) - ($right; $bottom)]"
        }
    }

    companion object {
        fun isLaidOut(view: View): Boolean {
            return view.isLaidOut
        }
    }
}