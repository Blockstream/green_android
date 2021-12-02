package com.blockstream.green.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.BaseProgressIndicator
import java.util.*
import kotlin.concurrent.schedule
import kotlin.properties.Delegates


class LongClickButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {
    private var timer: Timer? = null
    var clickListener: OnClickListener? = null

    var progressIndicator: BaseProgressIndicator<*>? by Delegates.observable(null) { _, _, newValue ->
        progressIndicator?.setProgressCompat(0, false)
    }

    init {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    progressIndicator?.setProgressCompat(100, true)

                    timer = Timer().also {
                        it.schedule(1000) {
                            post { // run in ui thread
                                performClick()
                                progressIndicator?.setProgressCompat(0, false)
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    progressIndicator?.setProgressCompat(0, true)
                    timer?.cancel()
                }
            }

            true
        }
    }


    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        if (enabled) {
            progressIndicator?.show()
        } else {
            timer?.cancel()
            progressIndicator?.hide()
        }
    }
}