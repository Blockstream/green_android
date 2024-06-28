package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.deviceIcon
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.data.AppEvent
import com.blockstream.green.databinding.DeviceScanFragmentBinding
import com.blockstream.green.extensions.navigate
import com.blockstream.green.gdk.getIcon
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

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if (sideEffect is SideEffects.Navigate){
            if(sideEffect.data is Pair<*, *>){
                @Suppress("UNCHECKED_CAST")
                val data : Pair<GreenWallet, GreenDevice> = sideEffect.data as Pair<GreenWallet, GreenDevice>

                NavGraphDirections.actionGlobalLoginFragment(wallet = data.first, deviceId = data.second.connectionIdentifier).let { navDirections ->
                    navigate(findNavController(), navDirections.actionId, navDirections.arguments, isLogout = true)
                }
            }
        } else if (sideEffect is SideEffects.OpenDialog) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.id_new_jade_firmware_required )
                .setCancelable(false)
                .setMessage(R.string.id_please_upgrade_your_jade_firmware_to)
                .setPositiveButton(R.string.id_ok) { _, _ ->
                    popBackStack()
                }
                .setNeutralButton(R.string.id_read_more) { _, _ ->
                    popBackStack()
                    openBrowser(Urls.HELP_JADE_USB_UPGRADE)
                }.show()
        } else if (sideEffect is AbstractDeviceViewModel.LocalSideEffects.RequestWalletIsDifferent) {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = viewModel

        viewModel.deviceLiveData.observe(viewLifecycleOwner) {
            binding.deviceImageView.setImageResource(
                it?.getIcon() ?: wallet.deviceIdentifiers?.firstOrNull()?.brand?.deviceIcon() ?: 0
            )
        }

        binding.buttonEnableBluetooth.setOnClickListener {
            enableBluetooth()
        }

        binding.buttonRequestPermission.setOnClickListener {
            requestPermissions()
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