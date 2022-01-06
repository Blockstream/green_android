package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.blockstream.gdk.data.Device
import com.blockstream.green.R
import com.blockstream.green.databinding.DeviceInteractionRequestBottomSheetBinding
import com.blockstream.green.utils.bounceDown
import com.blockstream.green.utils.dismissIn
import com.blockstream.green.utils.stringFromIdentifier
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.subscribeBy

class DeviceInteractionRequestBottomSheetDialogFragment constructor(
    val device: Device,
    val completable: Completable? = null,
    val text: String?,
    val delay: Long = 3000
) : BottomSheetDialogFragment() {

    private lateinit var binding: DeviceInteractionRequestBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.device_interaction_request_bottom_sheet,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner

        binding.device = device
        binding.text = if(text.isNullOrBlank()) null else requireContext().stringFromIdentifier(text) ?: text

        binding.arrow.bounceDown()

        completable?.subscribeBy(
            onError = {
                it.printStackTrace()
                dismiss()
            },
            onComplete = {
                dismiss()
            }
        )

        if(completable == null){
            dismissIn(delay)
        }

        return binding.root
    }
}
