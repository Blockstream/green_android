package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.databinding.RenameAccountBottomSheetBinding
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.nameCleanup
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class RenameAccountBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RenameAccountBottomSheetBinding, AbstractWalletViewModel>() {
    override val screenName = "RenameAccount"

    override fun inflate(layoutInflater: LayoutInflater) = RenameAccountBottomSheetBinding.inflate(layoutInflater)

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

    companion object : KLogging() {
        private const val SUBACCOUNT = "SUBACCOUNT"

        fun show(subAccount: SubAccount, fragmentManager: FragmentManager) {
            show(RenameAccountBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(Companion.SUBACCOUNT, subAccount)
                }
            }, fragmentManager)
        }
    }

}