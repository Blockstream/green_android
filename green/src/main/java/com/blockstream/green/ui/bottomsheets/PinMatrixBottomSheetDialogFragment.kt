package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.blockstream.green.databinding.PinMatrixBottomSheetBinding
import com.blockstream.green.utils.setNavigationResult
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
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
                if (pin.length < 9) {
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
            setNavigationResult(result = "", key = PIN_RESULT, destinationId = findNavController().currentDestination?.id)
            dismiss()
        }

        binding.buttonContinue.setOnClickListener {
            setNavigationResult(result = pin.toString(), key = PIN_RESULT, destinationId = findNavController().currentDestination?.id)
            dismiss()
        }
    }

    private fun updatePinView() {
        binding.pin = pin.indices.joinToString(" ") { "*" }
    }

    companion object : KLogging() {
        const val PIN_RESULT = "PIN_RESULT"

        fun show(fragmentManager: FragmentManager) {
            show(PinMatrixBottomSheetDialogFragment(), fragmentManager)
        }
    }
}