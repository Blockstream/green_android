package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.models.GreenViewModel
import com.blockstream.green.databinding.RenameAccountBottomSheetBinding
import com.blockstream.green.extensions.openKeyboard
import mu.KLogging

class RenameAccountBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RenameAccountBottomSheetBinding, GreenViewModel>() {
    override val screenName = "RenameAccount"

    override fun inflate(layoutInflater: LayoutInflater) = RenameAccountBottomSheetBinding.inflate(layoutInflater)

    override val isAdjustResize: Boolean = true

    override val accountOrNull: Account?
        get() = BundleCompat.getParcelable(requireArguments(), ACCOUNT, Account::class.java)

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
            viewModel.postEvent(Events.RenameAccount(account, binding.name.toString()))
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