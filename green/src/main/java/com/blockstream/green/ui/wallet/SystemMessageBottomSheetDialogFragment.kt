package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.SystemMessageBottomSheetBinding
import com.blockstream.green.gdk.observable
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.utils.errorDialog
import com.greenaddress.greenbits.wallets.HardwareCodeResolver
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@AndroidEntryPoint
class SystemMessageBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<SystemMessageBottomSheetBinding>(
        layout = R.layout.system_message_bottom_sheet
    ) {
    companion object {
        private const val SYSTEM_MESSAGE = "SYSTEM_MESSAGE"

        fun newInstance(message: String): SystemMessageBottomSheetDialogFragment =
            SystemMessageBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(SYSTEM_MESSAGE, message)
                }
            }
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState).also {
            val message = arguments?.getString(SYSTEM_MESSAGE) ?: ""

            binding.vm = viewModel

            binding.message = message

            binding.buttonAccept.setOnClickListener {
                viewModel.ackSystemMessage(message)
            }

            binding.buttonClose.setOnClickListener {
                dismiss()
            }

            viewModel.onEvent.observe(viewLifecycleOwner) {
                dismiss()
            }

            viewModel.onError.observe(viewLifecycleOwner) {
                it?.getContentIfNotHandledOrReturnNull()?.let {
                    errorDialog(it)
                }
            }
        }
    }
}