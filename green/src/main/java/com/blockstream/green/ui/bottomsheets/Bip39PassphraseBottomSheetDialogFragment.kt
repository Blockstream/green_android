package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.common.Urls
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.green.databinding.Bip39PassphraseBottomSheetBinding
import com.blockstream.green.utils.openBrowser
import mu.KLogging
import org.koin.android.ext.android.inject


class Bip39PassphraseBottomSheetDialogFragment: WalletBottomSheetDialogFragment<Bip39PassphraseBottomSheetBinding, LoginViewModel>(){
    private val settingsManager: SettingsManager by inject()

    override val segmentation: HashMap<String, Any>? = null

    override val screenName = "BIP39Passphrase"

    override fun inflate(layoutInflater: LayoutInflater) = Bip39PassphraseBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.passphrase = viewModel.bip39Passphrase.value

        binding.alwaysAskSwitch.isChecked = viewModel.greenWallet.askForBip39Passphrase

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
            viewModel.postEvent(LoginViewModel.LocalEvents.Bip39Passphrase(binding.passphrase ?: "", binding.alwaysAskSwitch.isChecked))
            dismiss()
        }
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(Bip39PassphraseBottomSheetDialogFragment(), fragmentManager)
        }
    }
}