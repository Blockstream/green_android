package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.deviceIcon
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.data.AppEvent
import com.blockstream.green.databinding.DeviceScanFragmentBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.extensions.navigate
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class DeviceScanFragment : AbstractDeviceFragment<DeviceScanFragmentBinding>(
    layout = R.layout.device_scan_fragment,
    menuRes = 0
) {
    sealed class DeviceScanFragmentEvent : AppEvent {
        data object RequestWalletIsDifferent: DeviceScanFragmentEvent()
    }

    private val args: DeviceScanFragmentArgs by navArgs()

    override val screenName = "DeviceScan"

    val wallet get() = viewModel.wallet

    override val title: String
        get() = wallet.name

    override val viewModel: DeviceScanViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override fun getAppViewModel(): AppViewModelAndroid = viewModel

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if (sideEffect is SideEffects.Navigate){
            if(sideEffect.data is Pair<*, *>){
                @Suppress("UNCHECKED_CAST")
                val data : Pair<GreenWallet, Device> = sideEffect.data as Pair<GreenWallet, Device>

                NavGraphDirections.actionGlobalLoginFragment(wallet = data.first, deviceId = data.second.id).let { navDirections ->
                    navigate(findNavController(), navDirections.actionId, navDirections.arguments, isLogout = true)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = viewModel

        viewModel.onEvent.observe(viewLifecycleOwner) { onEvent ->
            onEvent.getContentIfNotHandledForType<DeviceScanFragmentEvent>()?.let {
                if(it is DeviceScanFragmentEvent.RequestWalletIsDifferent){
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.id_warning)
                        .setMessage("The wallet hash is different from the previous wallet. Continue with a new Ephemeral Wallet?")
                        .setPositiveButton(R.string.id_continue) { _, _ ->
                            viewModel.requestUserActionEmitter?.complete(true)
                        }.setNegativeButton(android.R.string.cancel) { _, _ ->
                            viewModel.requestUserActionEmitter?.complete(false)
                        }
                        .setOnDismissListener {
                            viewModel.requestUserActionEmitter?.complete(false)
                        }
                        .show()
                }
            }
        }

        viewModel.deviceLiveData.observe(viewLifecycleOwner) {
            binding.deviceImageView.setImageResource(
                it?.getIcon() ?: wallet.deviceIdentifiers?.firstOrNull()?.brand?.deviceIcon() ?: 0
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
            if(wallet.isMainnet) gdk.networks().bitcoinElectrum else gdk.networks().testnetBitcoinElectrum
        )
    }

    override fun updateToolbar() {
        super.updateToolbar()
        toolbar.setButton(button = getString(R.string.id_troubleshoot)) {
            openBrowser(Urls.JADE_TROUBLESHOOT)
        }
    }
}