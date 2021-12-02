package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.databinding.RenameAccountBottomSheetBinding
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.ui.overview.OverviewFragment
import com.blockstream.green.utils.nameCleanup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RenameAccountBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RenameAccountBottomSheetBinding, AbstractWalletViewModel>(
    layout = R.layout.rename_account_bottom_sheet
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.name = (parentFragment as OverviewFragment).viewModel.getSubAccountLiveData().value?.name

        binding.buttonSave.setOnClickListener {
            viewModel.renameSubAccount(viewModel.session.activeAccount, binding.name.nameCleanup() ?: "")
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }
}