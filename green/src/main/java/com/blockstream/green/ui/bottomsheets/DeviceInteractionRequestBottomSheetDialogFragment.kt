package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.gdk.data.Device
import com.blockstream.green.databinding.DeviceInteractionRequestBottomSheetBinding
import com.blockstream.green.extensions.dismissIn
import com.blockstream.green.extensions.stringFromIdentifier
import com.blockstream.green.utils.bounceDown
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred

@AndroidEntryPoint
class DeviceInteractionRequestBottomSheetDialogFragment constructor(
    val device: Device,
    val completable: CompletableDeferred<Boolean>? = null,
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
        
        if(completable == null){
            dismissIn(delay)
        }else{
            lifecycleScope.launchWhenResumed {
                try {
                    completable.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                dismiss()
            }
        }
    }
}
