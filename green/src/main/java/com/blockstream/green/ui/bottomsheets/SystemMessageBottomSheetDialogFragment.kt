package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.databinding.SystemMessageBottomSheetBinding
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.dismissIn
import com.blockstream.green.utils.errorDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SystemMessageBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<SystemMessageBottomSheetBinding, AbstractWalletViewModel>() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override val screenName = "SystemMessage"

    override fun inflate(layoutInflater: LayoutInflater) =
        SystemMessageBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val message = arguments?.getString(SYSTEM_MESSAGE) ?: ""

        binding.vm = viewModel

        binding.message = message

        binding.buttonAccept.setOnClickListener {
            viewModel.ackSystemMessage(message)
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
        private const val SYSTEM_MESSAGE = "SYSTEM_MESSAGE"

        fun show(message: String, fragmentManager: FragmentManager) {
            show(SystemMessageBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(SYSTEM_MESSAGE, message)
                }
            }, fragmentManager)
        }
    }
}