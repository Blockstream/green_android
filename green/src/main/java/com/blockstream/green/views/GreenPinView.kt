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

        binding.shuffleListener = OnClickListener { view ->
            var digitsValues: Array<String> = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            var parentRoot = view.rootView
            // get all PIN buttons
            var button0 = parentRoot.findViewById<Button>(R.id.button0);
            var button1 = parentRoot.findViewById<Button>(R.id.button1);
            var button2 = parentRoot.findViewById<Button>(R.id.button2);
            var button3 = parentRoot.findViewById<Button>(R.id.button3);
            var button4 = parentRoot.findViewById<Button>(R.id.button4);
            var button5 = parentRoot.findViewById<Button>(R.id.button5);
            var button6 = parentRoot.findViewById<Button>(R.id.button6);
            var button7 = parentRoot.findViewById<Button>(R.id.button7);
            var button8 = parentRoot.findViewById<Button>(R.id.button8);
            var button9 = parentRoot.findViewById<Button>(R.id.button9);
            // shuffle digits values array
            digitsValues.shuffle()
            // set new buttons values
            button0.text = digitsValues[0];
            button1.text = digitsValues[1];
            button2.text = digitsValues[2];
            button3.text = digitsValues[3];
            button4.text = digitsValues[4];
            button5.text = digitsValues[5];
            button6.text = digitsValues[6];
            button7.text = digitsValues[7];
            button8.text = digitsValues[8];
            button9.text = digitsValues[9];
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

    private fun deletePinDigit(deleteAllDigits: Boolean){
        if(pin.isNotEmpty()){
            if(deleteAllDigits){
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
