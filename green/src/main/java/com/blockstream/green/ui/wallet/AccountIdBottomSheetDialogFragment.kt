package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.R
import com.blockstream.green.databinding.AccountIdBottomSheetBinding
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.utils.copyToClipboard
import com.blockstream.green.utils.pulse
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountIdBottomSheetDialogFragment(private val subAccount: SubAccount) : WalletBottomSheetDialogFragment<AccountIdBottomSheetBinding>(
    layout = R.layout.account_id_bottom_sheet
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.accountId = subAccount.receivingId

        binding.card.setOnClickListener {
            copyToClipboard("AccountID", subAccount.receivingId, requireContext())
            binding.textView.pulse()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }
}