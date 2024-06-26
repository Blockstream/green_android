package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.models.GreenViewModel
import com.blockstream.green.databinding.TransactionVerifyAddressBottomSheetBinding
import com.blockstream.green.utils.bounceDown
import mu.KLogging

class VerifyTransactionBottomSheetDialogFragment: WalletBottomSheetDialogFragment<TransactionVerifyAddressBottomSheetBinding, GreenViewModel>() {

    override val screenName = "VerifyTransaction"

    override val expanded: Boolean = true

    override fun inflate(layoutInflater: LayoutInflater) = TransactionVerifyAddressBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = true

        viewModel.session.ifConnected {
            binding.device = viewModel.session.gdkHwWallet?.device
        }


//        val fastAdapter = FastAdapter.with((parentFragment as SendConfirmFragment2).createAdapter(isAddressVerificationOnDevice = true))

//        binding.recycler.apply {
//            adapter = fastAdapter
//        }

        binding.arrow.bounceDown()
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(VerifyTransactionBottomSheetDialogFragment(), fragmentManager)
        }

        fun closeAll(fragmentManager: FragmentManager){
            closeAll(VerifyTransactionBottomSheetDialogFragment::class.java, fragmentManager)
        }
    }
}