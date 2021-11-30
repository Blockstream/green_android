package com.blockstream.green.ui

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.blockstream.green.databinding.PinMatrixBottomSheetBinding
import com.blockstream.green.utils.setNavigationResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class PinMatrixBottomSheetDialogFragment: BottomSheetDialogFragment(){

    private lateinit var binding: PinMatrixBottomSheetBinding

    val pin = StringBuffer()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PinMatrixBottomSheetBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner

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
            buttons[i].setOnClickListener { view: View ->
                if (pin.length < 9) {
                    pin.append(i + 1)
                    updatePinView()
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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


        return binding.root
    }

    private fun updatePinView() {
        binding.pin = pin.indices.joinToString(" ") { "*" }
    }

    companion object : KLogging() {
        const val PIN_RESULT = "PIN_RESULT"
    }
}