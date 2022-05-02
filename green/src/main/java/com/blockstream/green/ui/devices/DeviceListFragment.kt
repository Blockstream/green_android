package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.DeviceBrand
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.databinding.DeviceListFragmentBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.utils.openBrowser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class DeviceListFragment : AppFragment<DeviceListFragmentBinding>(
    layout = R.layout.device_list_fragment,
    menuRes = 0
), DeviceListCommon {
    private val args: DeviceListFragmentArgs by navArgs()

    override val screenName = "DeviceList"

    @Inject
    lateinit var viewModelFactory: DeviceListViewModel.AssistedFactory

    val viewModel: DeviceListViewModel by viewModels {
        DeviceListViewModel.provideFactory(
            viewModelFactory,
            args.deviceBrand
        )
    }

    @Inject
    lateinit var deviceManager: DeviceManager

    override val title: String
        get() = if (args.deviceBrand != DeviceBrand.Blockstream) args.deviceBrand.brand else ""

    override var requestPermission: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        // Nothing to do here, it's already handled by DeviceManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel
        binding.common.vm = viewModel

        init(fragment = this, binding = binding.common, viewModel = viewModel, settingsManager = settingsManager)

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            deviceManager.refreshDevices()
        }
    }

    override fun updateToolbar() {
        super.updateToolbar()

        if (args.deviceBrand == DeviceBrand.Blockstream) {
            toolbar.logo = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.blockstream_jade_logo
            )
            toolbar.setButton(button = getString(R.string.id_get_jade)) {
                openBrowser(settingsManager.getApplicationSettings(), Urls.JADE_STORE)
            }
        } else {
            toolbar.logo = ContextCompat.getDrawable(
                requireContext(),
                args.deviceBrand.icon
            )
            toolbar.setButton(button = getString(R.string.id_blockstream_store)) {
                openBrowser(settingsManager.getApplicationSettings(), Urls.HARDWARE_STORE)
            }
        }
    }

    override fun selectDevice(device: Device) {
        navigate(DeviceListFragmentDirections.actionDeviceListFragmentToDeviceInfoFragment(deviceId = device.id))
    }
}