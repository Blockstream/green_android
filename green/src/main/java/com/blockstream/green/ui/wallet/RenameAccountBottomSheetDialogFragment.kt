package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.databinding.RenameAccountBottomSheetBinding
import com.blockstream.green.utils.errorDialog
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.gdk.observable
import com.blockstream.green.utils.nameCleanup
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.kotlin.subscribeBy

@AndroidEntryPoint
class RenameAccountBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RenameAccountBottomSheetBinding>(
    layout = R.layout.rename_account_bottom_sheet
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.session.observable {
             it.getSubAccount(viewModel.wallet.activeAccount).result<SubAccount>()
        }.subscribeBy(
            onError = {
                errorDialog(it)
                dismiss()
            },
            onSuccess = {
                binding.name = it.name
            }
        )

        binding.buttonSave.setOnClickListener {
            viewModel.renameSubAccount(viewModel.wallet.activeAccount, binding.name.nameCleanup() ?: "")
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }
}