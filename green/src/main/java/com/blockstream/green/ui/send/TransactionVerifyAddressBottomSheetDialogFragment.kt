package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.View
import com.blockstream.gdk.data.Device
import com.blockstream.gdk.data.DeviceSupportsAntiExfilProtocol
import com.blockstream.gdk.data.DeviceSupportsLiquid
import com.blockstream.green.R
import com.blockstream.green.databinding.TransactionVerifyAddressBottomSheetBinding
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.utils.bounceDown
import com.mikepenz.fastadapter.FastAdapter

class TransactionVerifyAddressBottomSheetDialogFragment: WalletBottomSheetDialogFragment<TransactionVerifyAddressBottomSheetBinding, SendConfirmViewModel>(
layout = R.layout.transaction_verify_address_bottom_sheet
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = true

        viewModel.also { receiveViewModel ->
            receiveViewModel.deviceAddressValidationEvent.observe(viewLifecycleOwner) {
                it?.getContentIfNotHandledOrReturnNull()?.let {
                    dismiss()
                }
            }

            binding.device = receiveViewModel.session.hwWallet?.device ?: Device("Jade", true ,true, true,DeviceSupportsLiquid.Full, DeviceSupportsAntiExfilProtocol.Optional)
        }

        val fastAdapter = FastAdapter.with((parentFragment as SendConfirmFragment).createAdapter(isAddressVerificationOnDevice = true))

        binding.recycler.apply {
            adapter = fastAdapter
        }

        binding.arrow.bounceDown()
    }
}