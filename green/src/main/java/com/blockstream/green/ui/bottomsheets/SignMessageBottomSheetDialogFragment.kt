package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.databinding.SignMessageBottomSheetBinding
import com.blockstream.green.extensions.copyToClipboard
import com.blockstream.green.ui.addresses.AddressesViewModel
import com.blockstream.green.utils.getClipboard
import mu.KLogging

class SignMessageBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<SignMessageBottomSheetBinding, AddressesViewModel>() {

    override val screenName = "SignMessage"

    override val isAdjustResize: Boolean = true

    override fun inflate(layoutInflater: LayoutInflater) =
        SignMessageBottomSheetBinding.inflate(layoutInflater)

    val address: String
        get() = requireArguments().getString(ADDRESS, "")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.address = address
        binding.message = null

        binding.buttonPaste.setOnClickListener {
            binding.message = getClipboard(requireContext())
        }

        binding.buttonClear.setOnClickListener {
            binding.message = null
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonSign.setOnClickListener {
            signMessage()
        }

        binding.messageCard.setOnClickListener {
            copyToClipboard("Message", binding.message ?: "", animateView = binding.messageCard, showCopyNotification = true)
        }

        binding.signatureCard.setOnClickListener {
            copyToClipboard("Address", binding.signature ?: "", animateView = binding.signatureCard, showCopyNotification = true)
        }
    }

    private fun signMessage() {
        viewModel.signMessage(address, binding.message ?: ""){
            binding.signature = it
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