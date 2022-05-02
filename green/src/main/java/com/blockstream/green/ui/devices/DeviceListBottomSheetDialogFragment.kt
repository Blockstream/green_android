package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.databinding.DeviceListCommonBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.bottomsheets.AbstractBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeviceListBottomSheetDialogFragment : AbstractBottomSheetDialogFragment<DeviceListCommonBinding>(), DeviceListCommon{
    override val screenName = "DeviceListModal"

    override fun inflate(layoutInflater: LayoutInflater) = DeviceListCommonBinding.inflate(layoutInflater)

    @Inject
    lateinit var deviceManager: DeviceManager

    @Inject
    lateinit var greenWallet: GreenWallet

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var viewModelFactory: DeviceListViewModel.AssistedFactory

    val viewModel: DeviceListViewModel by viewModels {
        DeviceListViewModel.provideFactory(viewModelFactory, null)
    }

    override var requestPermission: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        // Nothing to do here, it's already handled by DeviceManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        init(fragment = this, binding = binding, viewModel = viewModel, settingsManager = settingsManager)

        binding.recycler.itemAnimator = null

        binding.constraintLayout.layoutParams = binding.constraintLayout.layoutParams.also {
            it.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        binding.recycler.layoutParams = binding.recycler.layoutParams.also {
            it.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    override fun onResume() {
        super.onResume()
        deviceManager.startBluetoothScanning()
    }

    override fun onPause() {
        super.onPause()
        deviceManager.pauseBluetoothScanning()
    }

    override fun selectDevice(device: Device) {

    }

    companion object {
        fun show(fragmentManager: FragmentManager) {
            show(DeviceListBottomSheetDialogFragment(), fragmentManager)
        }
    }
}