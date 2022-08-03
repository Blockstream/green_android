package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.DeviceScanFragmentBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class DeviceScanFragment : AbstractWalletFragment<DeviceScanFragmentBinding>(
    layout = R.layout.device_scan_fragment,
    menuRes = 0
) {
    private val args: DeviceScanFragmentArgs by navArgs()

    override val screenName = "DeviceScan"

    @Inject
    lateinit var viewModelFactory: DeviceScanViewModel.AssistedFactory

    val viewModel: DeviceScanViewModel by viewModels {
        DeviceScanViewModel.provideFactory(
            viewModelFactory,
            args.wallet
        )
    }
    override val walletOrNull by lazy { args.wallet }

    override fun isLoggedInRequired(): Boolean = false

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        viewModel.onEvent.observe(viewLifecycleOwner) { onEvent ->
            onEvent.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                val device = it.data as Device
                if (device.hasPermissionsOrIsBonded() || device.handleBondingByHwwImplementation()) {
                    navigate(device)
                }else{
                    viewModel.askForPermissionOrBond(device)
                }
            }
        }
    }

    private fun navigate(device: Device){
        navigate(
            DeviceScanFragmentDirections.actionDeviceScanFragmentToDeviceInfoFragment(wallet = viewModel.wallet, deviceId = device.id, autoLogin = true)
        )
    }
}