package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import com.blockstream.common.gdk.data.Device
import com.blockstream.green.databinding.DeviceInteractionRequestBottomSheetBinding
import com.blockstream.green.extensions.dismissIn
import com.blockstream.green.extensions.stringFromIdentifier
import com.blockstream.green.utils.bounceDown
import mu.KLogging


class DeviceInteractionRequestBottomSheetDialogFragment constructor() : AbstractBottomSheetDialogFragment<DeviceInteractionRequestBottomSheetBinding>() {

    override val screenName: String? = null

    override fun inflate(layoutInflater: LayoutInflater) = DeviceInteractionRequestBottomSheetBinding.inflate(layoutInflater)

    private val device by lazy { BundleCompat.getParcelable(requireArguments(), DEVICE, Device::class.java) }
    private val message by lazy { requireArguments().getString(MESSAGE, null)}
    private val delay by lazy { requireArguments().getLong(DELAY, 0)}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.device = device
        binding.text = if(message.isNullOrBlank()) null else requireContext().stringFromIdentifier(message)

        binding.arrow.bounceDown()

        if(delay > 0){
            dismissIn(delay)
        }
    }

    companion object : KLogging() {
        const val DEVICE = "DEVICE"
        const val MESSAGE = "MESSAGE"
        const val DELAY = "DELAY"

        fun showSingle(device: Device, message: String?, delay: Long, fragmentManager: FragmentManager){

            showSingle(DeviceInteractionRequestBottomSheetDialogFragment().also {
                it.arguments = Bundle().apply {
                    putParcelable(DEVICE, device)
                    putString(MESSAGE, message)
                    putLong(DELAY, delay)
                }
            }, fragmentManager)
        }

        fun closeAll(fragmentManager: FragmentManager){
            closeAll(DeviceInteractionRequestBottomSheetDialogFragment::class.java, fragmentManager)
        }
    }
}
