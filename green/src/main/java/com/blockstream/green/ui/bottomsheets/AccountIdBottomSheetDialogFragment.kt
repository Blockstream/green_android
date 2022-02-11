package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.databinding.AccountIdBottomSheetBinding
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.copyToClipboard
import com.blockstream.green.utils.pulse
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class AccountIdBottomSheetDialogFragment : WalletBottomSheetDialogFragment<AccountIdBottomSheetBinding, AbstractWalletViewModel>() {

    override val screenName = "AccountID"

    override fun inflate(layoutInflater: LayoutInflater) = AccountIdBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subAccount: SubAccount = arguments?.getParcelable(ACCOUNT) ?: return

        binding.accountId = subAccount.receivingId

        binding.card.setOnClickListener {
            copyToClipboard("AccountID", subAccount.receivingId, requireContext())
            binding.textView.pulse()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }

    companion object : KLogging() {
        private const val ACCOUNT = "ACCOUNT"

        fun show(subAccount: SubAccount, fragmentManager: FragmentManager) {
            show(AccountIdBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(ACCOUNT, subAccount)
                }
            }, fragmentManager)
        }
    }
}