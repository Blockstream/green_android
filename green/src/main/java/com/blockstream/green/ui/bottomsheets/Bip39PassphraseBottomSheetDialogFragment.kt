package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.Urls
import com.blockstream.green.databinding.Bip39PassphraseBottomSheetBinding
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.wallet.LoginViewModel
import com.blockstream.green.utils.openBrowser
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject

@AndroidEntryPoint
class Bip39PassphraseBottomSheetDialogFragment: WalletBottomSheetDialogFragment<Bip39PassphraseBottomSheetBinding, LoginViewModel>(){
    @Inject
    lateinit var settingsManager: SettingsManager

    override val screenName = "BIP39Passphrase"

    override fun inflate(layoutInflater: LayoutInflater) = Bip39PassphraseBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.passphrase = viewModel.bip39Passphrase.value

        binding.alwaysAskSwitch.isChecked = viewModel.wallet.askForBip39Passphrase

        binding.buttonLearnMore.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.HELP_BIP39_PASSPHRASE)
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonClear.setOnClickListener {
            viewModel.bip39Passphrase.value = ""
            dismiss()
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.bip39Passphrase.value = binding.passphrase?.trim()
            viewModel.setBip39Passphrase(binding.passphrase, binding.alwaysAskSwitch.isChecked)
            dismiss()
        }
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(Bip39PassphraseBottomSheetDialogFragment(), fragmentManager)
        }
    }
}