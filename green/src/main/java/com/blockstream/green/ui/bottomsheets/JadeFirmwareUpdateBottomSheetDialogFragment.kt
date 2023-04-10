package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.R
import com.blockstream.green.databinding.JadeFirmwareUpgradeBottomSheetBinding
import com.blockstream.green.extensions.dismissIn
import com.blockstream.green.extensions.padHex
import com.blockstream.green.ui.devices.AbstractDeviceFragment
import com.blockstream.green.ui.devices.AbstractDeviceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JadeFirmwareUpdateBottomSheetDialogFragment : AbstractBottomSheetDialogFragment<JadeFirmwareUpgradeBottomSheetBinding>() {

    override val screenName: String? = null

    override fun inflate(layoutInflater: LayoutInflater) = JadeFirmwareUpgradeBottomSheetBinding.inflate(layoutInflater)

    override fun isCancelable(): Boolean {
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireParentFragment() as? AbstractDeviceFragment<*>)?.viewModel?.also { viewModel ->

            viewModel.onEvent.observe(viewLifecycleOwner) { onEvent ->
                onEvent.peekContent().also {
                    when (it) {
                        is AbstractDeviceViewModel.DeviceEvent.FirmwarePushedToDevice -> {
                            logger.info { "FirmwarePushedToDevice ${it.firmwareFileData.image} ${it.hash}" }
                            binding.firmware = getString(R.string.id_firmware_version_s, "${it.firmwareFileData.image.version} ${it.firmwareFileData.image.config}")
                            binding.hash = getString(R.string.id_hash_s, it.hash.padHex())

                        }
                        is AbstractDeviceViewModel.DeviceEvent.FirmwareUpdateProgress -> {
                            logger.info { "FirmwareUpdateProgress ${it.written}/${it.totalSize}" }
                            if(it.written > 0) {
                                binding.progress = ((it.written / it.totalSize.toFloat()) * 100).toInt()
                            }
                        }
                        is AbstractDeviceViewModel.DeviceEvent.FirmwareUpdateComplete -> {
                            dismissIn(3000L)
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun show(fragmentManager: FragmentManager){
            showSingle(JadeFirmwareUpdateBottomSheetDialogFragment(), fragmentManager)
        }
    }
}
