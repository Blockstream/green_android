package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.gdk.data.Account
import com.blockstream.green.databinding.RenameAccountBottomSheetBinding
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.nameCleanup
import com.blockstream.green.extensions.openKeyboard
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class RenameAccountBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RenameAccountBottomSheetBinding, AbstractWalletViewModel>() {
    override val screenName = "RenameAccount"

    override fun inflate(layoutInflater: LayoutInflater) = RenameAccountBottomSheetBinding.inflate(layoutInflater)

    override val isAdjustResize: Boolean = true

    override val accountOrNull: Account?
        get() = arguments?.getParcelable<Account>(ACCOUNT)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val account = accountOrNull ?: run {
            dismiss()
            return
        }

        binding.name = account.name

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonSave.setOnClickListener {
            viewModel.renameAccount(account, binding.name.nameCleanup() ?: "")
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.accountName.requestFocus()
        openKeyboard()
    }


    companion object : KLogging() {
        private const val ACCOUNT = "ACCOUNT"

        fun show(account: Account, fragmentManager: FragmentManager) {
            show(RenameAccountBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(ACCOUNT, account)
                }
            }, fragmentManager)
        }
    }

}