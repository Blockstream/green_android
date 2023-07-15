package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.common.events.Events
import com.blockstream.green.databinding.PassphraseBottomSheetBinding
import com.blockstream.green.ui.AppFragment
import mu.KLogging


class PassphraseBottomSheetDialogFragment: AbstractBottomSheetDialogFragment<PassphraseBottomSheetBinding>(){
    override val screenName = "Passphrase"

    override fun inflate(layoutInflater: LayoutInflater) = PassphraseBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.passphrase = ""

        isCancelable = false

        binding.buttonContinue.setOnClickListener {
            (requireParentFragment() as? AppFragment<*>)?.getGreenViewModel()?.postEvent(Events.DeviceRequestResponse(binding.passphrase?.trim() ?: ""))
            dismiss()
        }
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(PassphraseBottomSheetDialogFragment(), fragmentManager)
        }
    }
}