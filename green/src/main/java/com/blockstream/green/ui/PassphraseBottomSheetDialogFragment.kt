package com.blockstream.green.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.blockstream.green.databinding.PassphraseBottomSheetBinding
import com.blockstream.green.utils.setNavigationResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class PassphraseBottomSheetDialogFragment: BottomSheetDialogFragment(){

    private lateinit var binding: PassphraseBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PassphraseBottomSheetBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.passphrase = ""
        binding.passphraseConfirm = ""

        isCancelable = false

        binding.buttonCancel.setOnClickListener {
            setNavigationResult(result = true, key = PASSPHRASE_CANCEL_RESULT, destinationId = findNavController().currentDestination?.id)
            dismiss()
        }

        binding.buttonContinue.setOnClickListener {
            setNavigationResult(result = binding.passphrase?.trim(), key = PASSPHRASE_RESULT, destinationId = findNavController().currentDestination?.id)
            dismiss()
        }

        return binding.root
    }

    companion object : KLogging() {
        const val PASSPHRASE_RESULT = "PASSPHRASE_RESULT"
        const val PASSPHRASE_CANCEL_RESULT = "PASSPHRASE_CANCEL_RESULT"
    }
}