package com.blockstream.green.views

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.databinding.GreenPinViewBinding
import com.blockstream.green.utils.shake

class GreenPinView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: GreenPinViewBinding =
        GreenPinViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var pin = ""
    private var pinToBeVerified = ""

    var isVerifyMode = false
    var listener: GreenPinViewListener? = null

    init {
        binding.keysEnabled = true
        binding.deleteEnabled = false

        binding.clickListener = OnClickListener { view ->
            if (view.id == R.id.buttonDelete) {
                deletePinDigit(false)
            } else {
                setPinDigit((view as Button).text.toString())
            }
        }

        binding.buttonDelete.setOnLongClickListener {
            deletePinDigit(true)
            true
        }
    }

    private fun shakeIndicator(){
        binding.indicatorDots.shake()
    }

    fun reset(withShakeAnimation: Boolean) {
        deletePinDigit(true)
        if(withShakeAnimation){
            shakeIndicator()
        }
    }

    private fun deletePinDigit(deleteAllDigis: Boolean){
        if(pin.isNotEmpty()){
            if(deleteAllDigis){
                pin = ""
                deletePinDigit(false) // call recursive to clear the verify mode
            }else{
                pin = pin.substring(0, pin.length - 1)
            }

            listener?.onPinChange(pin.length, pin)
        }else if(isVerifyMode){
            pinToBeVerified = ""
            listener?.onChangeMode(false)
        }
        pinUpdated()
    }

    private fun setPinDigit(digit: String) {
        if(pin.length < 6){
            pin += digit

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
    }

    private fun pinUpdated() {
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
