package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.cleanup
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.green.databinding.RenameWalletBottomSheetBinding
import com.blockstream.green.extensions.openKeyboard
import com.blockstream.green.ui.AppFragment
import mu.KLogging

class RenameWalletBottomSheetDialogFragment :
    AbstractBottomSheetDialogFragment<RenameWalletBottomSheetBinding>() {

    private lateinit var wallet: GreenWallet

    override val screenName = "RenameWallet"

    override fun inflate(layoutInflater: LayoutInflater) =
        RenameWalletBottomSheetBinding.inflate(layoutInflater)

    override val isAdjustResize: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BundleCompat.getParcelable(requireArguments(), WALLET, GreenWallet::class.java)?.let {
            wallet = it
        } ?: run {
            dismiss()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.name = wallet.name

        binding.buttonSave.setOnClickListener {
            binding.name.cleanup().takeIf { it.isNotBlank() }?.also {
                renameWallet(it)
                dismiss()
            }
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }

    private fun renameWallet(name: String){
        (requireParentFragment() as? AppFragment<*>)?.getGreenViewModel()?.also {
            it.postEvent(
                Events.RenameWallet(
                    wallet,
                    name
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        binding.walletNameTextView.requestFocus()
        openKeyboard()
    }

    companion object : KLogging() {
        private const val WALLET = "WALLET"

        fun show(wallet: GreenWallet, fragmentManager: FragmentManager) {
            show(RenameWalletBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(WALLET, wallet)
                }
            }, fragmentManager)
        }
    }
}