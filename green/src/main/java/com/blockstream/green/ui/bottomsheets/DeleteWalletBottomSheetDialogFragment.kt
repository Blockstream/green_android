package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.green.databinding.DeleteWalletBottomSheetBinding
import com.blockstream.green.ui.AppFragment
import mu.KLogging

class DeleteWalletBottomSheetDialogFragment : AbstractBottomSheetDialogFragment<DeleteWalletBottomSheetBinding>() {

    override val screenName = "DeleteWallet"

    private lateinit var wallet: GreenWallet

    override fun inflate(layoutInflater: LayoutInflater) = DeleteWalletBottomSheetBinding.inflate(layoutInflater)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            BundleCompat.getParcelable(it, WALLET, GreenWallet::class.java)
        }?.let {
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
        (requireParentFragment() as? AppFragment<*>)?.getGreenViewModel()?.also {
            it.postEvent(Events.DeleteWallet(wallet))
        }
    }

    companion object : KLogging() {
        private const val WALLET = "WALLET"

        fun show(wallet: GreenWallet, fragmentManager: FragmentManager) {
            show(DeleteWalletBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(WALLET, wallet)
                }
            }, fragmentManager)
        }
    }
}