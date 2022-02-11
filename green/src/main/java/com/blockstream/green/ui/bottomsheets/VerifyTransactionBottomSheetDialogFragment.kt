package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.databinding.TransactionVerifyAddressBottomSheetBinding
import com.blockstream.green.ui.send.SendConfirmFragment
import com.blockstream.green.ui.send.SendConfirmViewModel
import com.blockstream.green.utils.bounceDown
import com.mikepenz.fastadapter.FastAdapter
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class VerifyTransactionBottomSheetDialogFragment: WalletBottomSheetDialogFragment<TransactionVerifyAddressBottomSheetBinding, SendConfirmViewModel>() {

    override val screenName = "VerifyTransaction"

    override fun inflate(layoutInflater: LayoutInflater) = TransactionVerifyAddressBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = true

        viewModel.also { receiveViewModel ->
            receiveViewModel.deviceAddressValidationEvent.observe(viewLifecycleOwner) {
                it?.getContentIfNotHandledOrReturnNull()?.let {
                    dismiss()
                }
            }

            binding.device = receiveViewModel.session.hwWallet?.device
        }

        val fastAdapter = FastAdapter.with((parentFragment as SendConfirmFragment).createAdapter(isAddressVerificationOnDevice = true))

        binding.recycler.apply {
            adapter = fastAdapter
        }

        binding.arrow.bounceDown()
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(VerifyTransactionBottomSheetDialogFragment(), fragmentManager)
        }
    }
}