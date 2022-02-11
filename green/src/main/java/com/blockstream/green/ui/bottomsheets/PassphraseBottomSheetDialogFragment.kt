package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.blockstream.green.databinding.PassphraseBottomSheetBinding
import com.blockstream.green.utils.setNavigationResult
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class PassphraseBottomSheetDialogFragment: AbstractBottomSheetDialogFragment<PassphraseBottomSheetBinding>(){
    override val screenName = "Passphrase"

    override fun inflate(layoutInflater: LayoutInflater) = PassphraseBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
    }

    companion object : KLogging() {
        const val PASSPHRASE_RESULT = "PASSPHRASE_RESULT"
        const val PASSPHRASE_CANCEL_RESULT = "PASSPHRASE_CANCEL_RESULT"

        fun show(fragmentManager: FragmentManager){
            show(PassphraseBottomSheetDialogFragment(), fragmentManager)
        }
    }
}