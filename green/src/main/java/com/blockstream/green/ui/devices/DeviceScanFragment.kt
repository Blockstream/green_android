package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.base.Urls
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.DeviceScanFragmentBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.extensions.navigate
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.openBrowser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class DeviceScanFragment : AbstractDeviceFragment<DeviceScanFragmentBinding>(
    layout = R.layout.device_scan_fragment,
    menuRes = 0
) {
    private val args: DeviceScanFragmentArgs by navArgs()

    override val screenName = "DeviceScan"

    val wallet get() = viewModel.wallet

    override val title: String
        get() = wallet.name

    @Inject
    lateinit var viewModelFactory: DeviceScanViewModel.AssistedFactory

    override val viewModel: DeviceScanViewModel by viewModels {
        DeviceScanViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun getAppViewModel(): AppViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = viewModel

        viewModel.onEvent.observe(viewLifecycleOwner) { onEvent ->
            onEvent.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                if(it.data is Device){
                    NavGraphDirections.actionGlobalLoginFragment(wallet, deviceId = it.data.id).let { navDirections ->
                        navigate(findNavController(), navDirections.actionId, navDirections.arguments, isLogout = true)
                    }
                }
            }
        }

        viewModel.deviceLiveData.observe(viewLifecycleOwner) {
            binding.deviceImageView.setImageResource(
                it?.getIcon() ?: wallet.deviceIdentifiers?.firstOrNull()?.brand?.deviceIcon ?: 0
            )
        }

        binding.buttonEnableBluetooth.setOnClickListener {
            enableBluetooth()
        }

        binding.buttonRequestPermission.setOnClickListener {
            requestLocationPermission()
        }

        binding.buttonEnableLocationService.setOnClickListener {
            enableLocationService()
        }
    }

    override fun requestNetwork() {
        viewModel.requestNetworkEmitter?.complete(
            if(wallet.isMainnet) gdkBridge.networks.bitcoinElectrum else gdkBridge.networks.testnetBitcoinElectrum
        )
    }

    override fun updateToolbar() {
        super.updateToolbar()
        toolbar.setButton(button = getString(R.string.id_troubleshoot)) {
            openBrowser(Urls.JADE_TROUBLESHOOT)
        }
    }
}