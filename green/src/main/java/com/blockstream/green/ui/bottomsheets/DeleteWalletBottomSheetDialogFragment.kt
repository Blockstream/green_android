package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.DeleteWalletBottomSheetBinding
import com.blockstream.green.ui.intro.IntroFragment
import com.blockstream.green.ui.login.LoginFragment
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class DeleteWalletBottomSheetDialogFragment : AbstractBottomSheetDialogFragment<DeleteWalletBottomSheetBinding>() {

    override val screenName = "DeleteWallet"

    private lateinit var wallet: Wallet

    override fun inflate(layoutInflater: LayoutInflater) = DeleteWalletBottomSheetBinding.inflate(layoutInflater)
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

        binding.buttonDelete.setOnClickListener {
            binding.isConfirmed = true
        }

        binding.buttonDeleteConfirm.setOnClickListener {
            deleteWallet()
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }

    private fun deleteWallet(){
        requireParentFragment().let { fragment ->
            if(fragment is IntroFragment){
                fragment.viewModel.deleteWallet(wallet)
            }else if(fragment is LoginFragment){
                fragment.viewModel.deleteWallet()
            }
        }
    }

    companion object : KLogging() {
        private const val WALLET = "WALLET"

        fun show(wallet: Wallet, fragmentManager: FragmentManager) {
            show(DeleteWalletBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(WALLET, wallet)
                }
            }, fragmentManager)
        }
    }
}