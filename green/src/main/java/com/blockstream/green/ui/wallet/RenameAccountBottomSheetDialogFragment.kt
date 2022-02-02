package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.R
import com.blockstream.green.databinding.RenameAccountBottomSheetBinding
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.utils.nameCleanup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RenameAccountBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RenameAccountBottomSheetBinding, AbstractWalletViewModel>(
    layout = R.layout.rename_account_bottom_sheet
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subAccount = arguments?.getParcelable<SubAccount>(SUBACCOUNT) ?: run {
            dismiss()
            return
        }

        binding.name = subAccount.name

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonSave.setOnClickListener {
            viewModel.renameSubAccount(subAccount.pointer, binding.name.nameCleanup() ?: "")
            dismiss()
        }
    }

    companion object {
        private const val SUBACCOUNT = "SUBACCOUNT"

        fun newInstance(subAccount: SubAccount): RenameAccountBottomSheetDialogFragment =
            RenameAccountBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(SUBACCOUNT, subAccount)
                }
            }
    }
}