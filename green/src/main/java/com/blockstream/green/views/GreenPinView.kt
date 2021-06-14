package com.blockstream.green.views

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.databinding.GreenPinViewBinding
import com.blockstream.green.utils.copyToClipboard
import com.blockstream.green.utils.getClipboard
import com.blockstream.green.utils.shake
import com.google.android.material.snackbar.Snackbar

class GreenPinView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: GreenPinViewBinding =
        GreenPinViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var pin = ""
    private var pinToBeVerified = ""

    val showDigits : Boolean
    var isVerifyMode = false
    var listener: GreenPinViewListener? = null

    init {
        binding.keysEnabled = true
        binding.deleteEnabled = false

        binding.clickListener = OnClickListener { view ->
            if (view.id == R.id.buttonDelete) {
                deletePinDigit(false)
            } else if (view.id == R.id.buttonPaste) {
                paste()
            } else {
                setPinDigit((view as Button).text.toString())
            }
        }

        binding.buttonDelete.setOnLongClickListener {
            deletePinDigit(true)
            true
        }

        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.GreenPinView)

        binding.withPaste = attributes.getBoolean(R.styleable.GreenPinView_withPaste, false)
        binding.withShuffle = attributes.getBoolean(R.styleable.GreenPinView_withShuffle, false)
        showDigits = attributes.getBoolean(R.styleable.GreenPinView_showDigits, false).also {
            binding.showDigits = it
        }

        attributes.recycle()

        pinUpdated()

        val digitButtons = listOf(
            binding.button0,
            binding.button1,
            binding.button2,
            binding.button3,
            binding.button4,
            binding.button5,
            binding.button6,
            binding.button7,
            binding.button8,
            binding.button9
        )

        binding.shuffleListener = OnClickListener {
            // shuffle digits values array
            val digitsValues: List<String> = digitButtons.mapIndexed { index, _ -> "$index" }.shuffled()
            // set new buttons values
            digitButtons.forEachIndexed { index, button ->
                button.text = digitsValues[index]
            }
        }

        binding.shuffleLongClickListener = OnLongClickListener {
            // reset values
            digitButtons.forEachIndexed { index, button ->
                button.text = "$index"
            }
            true
        }
    }

    private fun shakeIndicator() {
        binding.indicatorDots.shake()
    }

    fun reset(withShakeAnimation: Boolean) {
        deletePinDigit(true)
        if (withShakeAnimation) {
            shakeIndicator()
        }
    }

    private fun paste() {
        getClipboard(context)?.let{
            if(it.length == 6 && it.isDigitsOnly()){
                pin = it
                validatePin()
            }else{
                Snackbar.make(this, R.string.id_invalid_clipboard_contents, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun deletePinDigit(deleteAllDigis: Boolean) {
        if (pin.isNotEmpty()) {
            if (deleteAllDigis) {
                pin = ""
                deletePinDigit(false) // call recursive to clear the verify mode
            } else {
                pin = pin.substring(0, pin.length - 1)
            }

            listener?.onPinChange(pin.length, pin)
        } else if (isVerifyMode) {
            pinToBeVerified = ""
            listener?.onChangeMode(false)
        }
        pinUpdated()
    }

    private fun setPinDigit(digit: String) {
        if (pin.length < 6) {
            pin += digit

            validatePin()
        }
    }

    private fun validatePin(){
        if (pin.length == 6) {
            if (isVerifyMode) {
                when {
                    pinToBeVerified.isEmpty() -> {
                        pinToBeVerified = pin
                        pin = ""
                        listener?.onChangeMode(true)
                    }
                    pin == pinToBeVerified -> {
                        listener?.onPin(pin)
                    }
                    else -> {
                        pin = ""
                        pinToBeVerified = ""
                        listener?.onPinNotVerified()
                        listener?.onChangeMode(false)
                        shakeIndicator()
                    }
                }
            } else {
                listener?.onPin(pin)
            }
        } else {
            listener?.onPinChange(pin.length, pin)
        }

        pinUpdated()
    }

    private fun pinUpdated() {

        if(showDigits) {
            // Create an array with space or the digit value
            binding.pin =
                (0 until 6).mapIndexed { index, _ -> pin.getOrElse(index) { ' ' }.toString() }
                    .toTypedArray()
        }
        binding.keysEnabled = pin.length < 6
        binding.deleteEnabled = pin.isNotEmpty() || pinToBeVerified.isNotEmpty()
        binding.pinLength = pin.length
    }

    fun setError(text: String?) {
        binding.error.text = text
        binding.error.isVisible = !text.isNullOrBlank()
    }
}

interface GreenPinViewListener {
    fun onPin(pin: String)
    fun onPinChange(pinLength: Int, intermediatePin: String?)
    fun onPinNotVerified()
    fun onChangeMode(isVerify: Boolean)
}
