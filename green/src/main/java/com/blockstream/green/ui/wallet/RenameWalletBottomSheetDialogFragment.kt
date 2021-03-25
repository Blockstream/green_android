package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.databinding.RenameWalletBottomSheetBinding
import com.blockstream.green.utils.nameCleanup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RenameWalletBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RenameWalletBottomSheetBinding>(
    layout = R.layout.rename_wallet_bottom_sheet
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.name = viewModel.wallet.name

        binding.buttonSave.setOnClickListener {
            viewModel.renameWallet(binding.name.nameCleanup() ?: "")
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }
}