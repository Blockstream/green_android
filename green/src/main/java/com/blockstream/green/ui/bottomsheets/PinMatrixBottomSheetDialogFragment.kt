package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.common.events.Events
import com.blockstream.green.databinding.PinMatrixBottomSheetBinding
import com.blockstream.green.ui.AppFragment
import mu.KLogging

class PinMatrixBottomSheetDialogFragment: AbstractBottomSheetDialogFragment<PinMatrixBottomSheetBinding>(){
    override val screenName = "PinMatrix"

    val pin = StringBuffer()

    override fun inflate(layoutInflater: LayoutInflater) = PinMatrixBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = false

        val buttons = listOf(
            binding.pinButton7,
            binding.pinButton8,
            binding.pinButton9,
            binding.pinButton4,
            binding.pinButton5,
            binding.pinButton6,
            binding.pinButton1,
            binding.pinButton2,
            binding.pinButton3,
        )

        for (i in buttons.indices) {
            buttons[i].setOnClickListener { button: View ->
                if (pin.length <= 50) {
                    pin.append(i + 1)
                    updatePinView()
                    button.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            }
        }

        binding.textInputLayout.setEndIconOnClickListener {
            if(pin.isNotEmpty()) {
                pin.deleteCharAt(pin.length - 1)
                updatePinView()
            }
        }

        binding.textInputLayout.setEndIconOnLongClickListener {
            if(pin.isNotEmpty()) {
                pin.delete(0, pin.length)
                updatePinView()
            }
            true
        }

        binding.buttonCancel.setOnClickListener {
            (requireParentFragment() as? AppFragment<*>)?.getGreenViewModel()?.postEvent(Events.DeviceRequestResponse(null))
            dismiss()
        }

        binding.buttonContinue.setOnClickListener {
            (requireParentFragment() as? AppFragment<*>)?.getGreenViewModel()?.postEvent(Events.DeviceRequestResponse(pin.toString()))
            dismiss()
        }
    }

    private fun updatePinView() {
        binding.pin = pin.indices.joinToString(" ") { "*" }
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager) {
            show(PinMatrixBottomSheetDialogFragment(), fragmentManager)
        }
    }
}