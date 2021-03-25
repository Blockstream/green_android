package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.databinding.DeleteWalletBottomSheetBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteWalletBottomSheetDialogFragment : WalletBottomSheetDialogFragment<DeleteWalletBottomSheetBinding>(
    layout = R.layout.delete_wallet_bottom_sheet
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.name = viewModel.wallet.name

        binding.buttonDelete.setOnClickListener {
            binding.isConfirmed = true
        }

        binding.buttonDeleteConfirm.setOnClickListener {
            viewModel.deleteWallet()
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }
}