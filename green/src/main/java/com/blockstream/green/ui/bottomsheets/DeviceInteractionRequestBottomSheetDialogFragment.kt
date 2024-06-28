package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import com.blockstream.common.Urls
import com.blockstream.common.gdk.data.Device
import com.blockstream.green.databinding.DeviceInteractionRequestBottomSheetBinding
import com.blockstream.green.extensions.dismissIn
import com.blockstream.green.extensions.stringFromIdentifier
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.utils.bounceDown
import com.blockstream.green.utils.openBrowser
import com.blockstream.common.utils.Loggable


class DeviceInteractionRequestBottomSheetDialogFragment constructor() : AbstractBottomSheetDialogFragment<DeviceInteractionRequestBottomSheetBinding>() {

    override val screenName: String? = null

    override fun inflate(layoutInflater: LayoutInflater) = DeviceInteractionRequestBottomSheetBinding.inflate(layoutInflater)

    private val device by lazy { BundleCompat.getParcelable(requireArguments(), DEVICE, Device::class.java) }
    private val message by lazy { requireArguments().getString(MESSAGE, null)}
    private val delay by lazy { requireArguments().getLong(DELAY, 0)}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.device = device
        if (message == "get_master_blinding_key") {
            binding.title = "id_green_needs_the_master_blinding_key"
            binding.message = "id_to_show_balances_and_transactions_on_liquid"
        } else {
            binding.title =
                if (message.isNullOrBlank()) null else requireContext().stringFromIdentifier(message)
        }

        binding.buttonLearnMore.setOnClickListener {
            (requireParentFragment() as? AppFragment<*>)?.openBrowser(Urls.HELP_MASTER_BLINDING_KEY)
        }

        binding.arrow.bounceDown()

        if(delay > 0){
            dismissIn(delay)
        }
    }

    companion object : Loggable() {
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
