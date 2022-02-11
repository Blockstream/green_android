package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.green.ApplicationScope
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.databinding.RenameWalletBottomSheetBinding
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
            lifecycleScope.launchWhenCreated {
                val name = binding.name.nameCleanup() ?: ""

                if (name.isBlank()) return@launchWhenCreated

                wallet.name = name
                walletRepository.updateWalletSuspend(wallet)

                countly.renameWallet()

                dismiss()
            }
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
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