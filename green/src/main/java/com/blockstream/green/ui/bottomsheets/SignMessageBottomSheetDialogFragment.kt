package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.common.events.Events
import com.blockstream.common.models.addresses.AddressesViewModel
import com.blockstream.common.models.addresses.SignMessageViewModel
import com.blockstream.green.databinding.SignMessageBottomSheetBinding
import com.blockstream.green.extensions.copyToClipboard
import com.blockstream.green.utils.getClipboard
import mu.KLogging
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SignMessageBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<SignMessageBottomSheetBinding, AddressesViewModel>() {

        // screenName in ViewModel
    override val screenName: String? = null

    val address: String
        get() = requireArguments().getString(ADDRESS, "")

    private val signMessageViewModel: SignMessageViewModel by viewModel {
        parametersOf(viewModel.greenWallet, viewModel.account.accountAsset, address)
    }

    override val isAdjustResize: Boolean = true

    override fun inflate(layoutInflater: LayoutInflater) =
        SignMessageBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = signMessageViewModel

        binding.buttonPaste.setOnClickListener {
            signMessageViewModel.message.value = getClipboard(requireContext()) ?: ""
        }

        binding.buttonClear.setOnClickListener {
            signMessageViewModel.message.value = ""
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonSign.setOnClickListener {
            signMessageViewModel.postEvent(Events.Continue)
        }

        binding.messageCard.setOnClickListener {
            copyToClipboard("Message", signMessageViewModel.message.value, animateView = binding.messageCard, showCopyNotification = true)
        }

        binding.signatureCard.setOnClickListener {
            copyToClipboard("Address", signMessageViewModel.signature.value ?: "", animateView = binding.signatureCard, showCopyNotification = true)
        }
    }

    companion object : KLogging() {
        private const val ADDRESS = "ADDRESS"

        fun show(address: String, fragmentManager: FragmentManager) {
            show(SignMessageBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(ADDRESS, address)
                }
            }, fragmentManager)
        }
    }
}