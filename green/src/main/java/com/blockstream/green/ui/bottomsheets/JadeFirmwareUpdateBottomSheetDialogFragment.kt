package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.extensions.padHex
import com.blockstream.common.utils.Loggable
import com.blockstream.green.R
import com.blockstream.green.databinding.JadeFirmwareUpgradeBottomSheetBinding
import com.blockstream.green.extensions.dismissIn
import com.blockstream.green.ui.devices.AbstractDeviceFragment
import com.blockstream.green.ui.devices.AbstractDeviceViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class JadeFirmwareUpdateBottomSheetDialogFragment : AbstractBottomSheetDialogFragment<JadeFirmwareUpgradeBottomSheetBinding>() {

    override val screenName: String? = null

    override fun inflate(layoutInflater: LayoutInflater) = JadeFirmwareUpgradeBottomSheetBinding.inflate(layoutInflater)

    override fun isCancelable(): Boolean {
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireParentFragment() as? AbstractDeviceFragment<*>)?.viewModel?.also { viewModel ->

            viewModel.firmwareState.filterNotNull().onEach {
                when (it) {
                    is AbstractDeviceViewModel.LocalSideEffects.FirmwarePushedToDevice -> {
                        logger.i { "FirmwarePushedToDevice ${it.firmwareFileData.image} ${it.hash}" }
                        binding.firmware = getString(R.string.id_firmware_version_s, "${it.firmwareFileData.image.version} ${it.firmwareFileData.image.config}")
                        binding.hash = getString(R.string.id_hash_s, it.hash.padHex())

                    }
                    is AbstractDeviceViewModel.LocalSideEffects.FirmwareUpdateProgress -> {
                        logger.i { "FirmwareUpdateProgress ${it.written}/${it.totalSize}" }
                        if(it.written > 0) {
                            binding.progress = ((it.written / it.totalSize.toFloat()) * 100).toInt()
                        }
                    }
                    is AbstractDeviceViewModel.LocalSideEffects.FirmwareUpdateComplete -> {
                        dismissIn(3000L)
                    }
                }
            }.launchIn(lifecycleScope)
        }
    }

    companion object : Loggable() {
        fun show(fragmentManager: FragmentManager){
            showSingle(JadeFirmwareUpdateBottomSheetDialogFragment(), fragmentManager)
        }
    }
}
