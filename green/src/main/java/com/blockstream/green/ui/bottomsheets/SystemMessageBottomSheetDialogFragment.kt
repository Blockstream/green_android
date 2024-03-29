package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import com.blockstream.common.gdk.data.Network
import com.blockstream.green.databinding.SystemMessageBottomSheetBinding
import com.blockstream.green.extensions.dismissIn
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.ui.wallet.AbstractWalletViewModel

class SystemMessageBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<SystemMessageBottomSheetBinding, AbstractWalletViewModel>() {

    override val screenName = "SystemMessage"

    override val expanded = true

    override fun inflate(layoutInflater: LayoutInflater) =
        SystemMessageBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val message = requireArguments().getString(MESSAGE) ?: ""

        binding.vm = viewModel

        binding.message = message

        binding.buttonAccept.setOnClickListener {
            BundleCompat.getParcelable(requireArguments(), NETWORK, Network::class.java)?.also {
                viewModel.ackSystemMessage(it, message)
            }
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<AbstractWalletViewModel.WalletEvent.AckMessage>()
                ?.let {
                    binding.closing = true
                    dismissIn(1000)
                }
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }
    }

    companion object {
        private const val NETWORK = "NETWORK"
        private const val MESSAGE = "MESSAGE"

        fun show(network: Network, message: String, fragmentManager: FragmentManager) {
            show(SystemMessageBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(NETWORK, network)
                    bundle.putString(MESSAGE, message)
                }
            }, fragmentManager)
        }
    }
}