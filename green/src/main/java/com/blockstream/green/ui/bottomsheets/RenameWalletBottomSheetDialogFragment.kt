package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.ApplicationScope
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.databinding.RenameWalletBottomSheetBinding
import com.blockstream.green.extensions.isNotBlank
import com.blockstream.green.extensions.openKeyboard
import com.blockstream.green.ui.intro.IntroFragment
import com.blockstream.green.ui.login.LoginFragment
import com.blockstream.green.utils.nameCleanup
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject

@AndroidEntryPoint
class RenameWalletBottomSheetDialogFragment :
    AbstractBottomSheetDialogFragment<RenameWalletBottomSheetBinding>() {

    private lateinit var wallet: Wallet

    @Inject
    lateinit var walletRepository: WalletRepository

    @Inject
    lateinit var applicationScope: ApplicationScope

    override val screenName = "RenameWallet"

    override fun inflate(layoutInflater: LayoutInflater) =
        RenameWalletBottomSheetBinding.inflate(layoutInflater)

    override val isAdjustResize: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<Wallet>(WALLET)?.let {
            wallet = it
        } ?: run {
            dismiss()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.name = wallet.name

        binding.buttonSave.setOnClickListener {
            binding.name.nameCleanup().takeIf { it.isNotBlank() }?.also {
                renameWallet(it)
                dismiss()
            }
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }

    private fun renameWallet(name: String){
        requireParentFragment().let { fragment ->
            if(fragment is IntroFragment){
                fragment.viewModel.renameWallet(name, wallet)
            }else if(fragment is LoginFragment){
                fragment.viewModel.renameWallet(name)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.walletNameTextView.requestFocus()
        openKeyboard()
    }

    companion object : KLogging() {
        private const val WALLET = "WALLET"

        fun show(wallet: Wallet, fragmentManager: FragmentManager) {
            show(RenameWalletBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(WALLET, wallet)
                }
            }, fragmentManager)
        }
    }
}