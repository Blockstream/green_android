package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.R
import com.blockstream.green.databinding.VerifyAddressBottomSheetBinding
import com.blockstream.green.ui.receive.ReceiveViewModel
import com.blockstream.green.utils.bounceDown
import com.blockstream.green.utils.dismissIn
import com.blockstream.green.utils.errorDialog
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging


@AndroidEntryPoint
class VerifyAddressBottomSheetDialogFragment : WalletBottomSheetDialogFragment<VerifyAddressBottomSheetBinding, ReceiveViewModel>() {

    override val screenName = "VerifyAddress"

    override fun inflate(layoutInflater: LayoutInflater) = VerifyAddressBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.also { receiveViewModel ->
            receiveViewModel.deviceAddressValidationEvent.observe(viewLifecycleOwner) {
                it?.getContentIfNotHandledOrReturnNull()?.let { addressMatch ->
                    responseFromDevice(addressMatch)
                }
            }

            binding.device = receiveViewModel.session.hwWallet?.device
        }

        binding.arrow.bounceDown()

        binding.buttonClose.setOnClickListener {
             dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        // Remove dim so that the address behind to be clearly visible
        dialog?.window?.setDimAmount(0F)
    }

    private fun responseFromDevice(addressMatch: Boolean){
        if(addressMatch){
            binding.confirmed = true
            dismissIn(2000)
        }else{
            errorDialog(getString(R.string.id_the_addresses_dont_match))
            dismiss()
        }
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager) {
            show(VerifyAddressBottomSheetDialogFragment(), fragmentManager)
        }
    }
}