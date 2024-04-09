package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.common.models.GreenViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.VerifyAddressBottomSheetBinding
import com.blockstream.green.extensions.dismissIn
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.utils.bounceDown
import mu.KLogging


class VerifyAddressBottomSheetDialogFragment : WalletBottomSheetDialogFragment<VerifyAddressBottomSheetBinding, GreenViewModel>() {

    override val screenName = "VerifyAddress"

    override fun inflate(layoutInflater: LayoutInflater) = VerifyAddressBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.also { receiveViewModel ->
//            receiveViewModel.deviceAddressValidationEvent.observe(viewLifecycleOwner) {
//                it?.getContentIfNotHandledOrReturnNull()?.let { addressMatch ->
//                    responseFromDevice(addressMatch)
//                }
//            }

            binding.device = receiveViewModel.session.gdkHwWallet?.device
        }

        binding.arrow.bounceDown()

        binding.address = requireArguments().getString(ADDRESS) ?: ""

        binding.buttonClose.setOnClickListener {
             dismiss()
        }
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
        private const val ADDRESS = "ADDRESS"

        fun show(address: String, fragmentManager: FragmentManager) {
            show(VerifyAddressBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(ADDRESS, address)
                }
            }, fragmentManager)
        }

        fun closeAll(fragmentManager: FragmentManager){
            closeAll(VerifyAddressBottomSheetDialogFragment::class.java, fragmentManager)
        }
    }
}