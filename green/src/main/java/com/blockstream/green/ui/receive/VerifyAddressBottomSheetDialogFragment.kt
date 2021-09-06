package com.blockstream.green.ui.receive

import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.blockstream.green.R
import com.blockstream.green.databinding.VerifyAddressBottomSheetBinding
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.utils.bounceDown
import com.blockstream.green.utils.dismissIn
import com.blockstream.green.utils.errorDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay


@AndroidEntryPoint
class VerifyAddressBottomSheetDialogFragment : WalletBottomSheetDialogFragment<VerifyAddressBottomSheetBinding>(
    layout = R.layout.verify_address_bottom_sheet
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (parentFragment as ReceiveFragment).viewModel.also { receiveViewModel ->
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
}