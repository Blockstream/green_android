package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.blockstream.gdk.data.Device
import com.blockstream.green.databinding.DeviceInteractionRequestBottomSheetBinding
import com.blockstream.green.utils.bounceDown
import com.blockstream.green.extensions.dismissIn
import com.blockstream.green.extensions.stringFromIdentifier
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

@AndroidEntryPoint
class DeviceInteractionRequestBottomSheetDialogFragment constructor(
    val device: Device,
    val completable: Completable? = null,
    val text: String?,
    val delay: Long = 3000
) : AbstractBottomSheetDialogFragment<DeviceInteractionRequestBottomSheetBinding>() {

    override val screenName: String? = null

    override fun inflate(layoutInflater: LayoutInflater) = DeviceInteractionRequestBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        )?.addTo(disposables)

        if(completable == null){
            dismissIn(delay)
        }
    }
}
