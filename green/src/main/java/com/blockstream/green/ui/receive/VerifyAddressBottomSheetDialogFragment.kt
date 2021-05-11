package com.blockstream.green.ui.receive

import android.os.Bundle
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.databinding.RequestAmountLabelBottomSheetBinding
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyAddressBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RequestAmountLabelBottomSheetBinding>(
    layout = R.layout.request_amount_label_bottom_sheet
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (parentFragment as ReceiveFragment).viewModel.also {
            binding.amount = it.requestAmount.value
            binding.label = it.label.value
        }


        binding.buttonOK.setOnClickListener {
            (parentFragment as ReceiveFragment).viewModel.setRequestAmountAndLabel(binding.amount, binding.label)
            dismiss()
        }

        binding.buttonClear.setOnClickListener {
            (parentFragment as ReceiveFragment).clearRequestAmountAndLabel()
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }
}