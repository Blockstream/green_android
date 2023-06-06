package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.deviceIcon
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.DeviceScanFragmentBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.extensions.navigate
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class DeviceScanFragment : AbstractDeviceFragment<DeviceScanFragmentBinding>(
    layout = R.layout.device_scan_fragment,
    menuRes = 0
) {
    sealed class DeviceScanFragmentEvent : AppEvent {
        object RequestWalletIsDifferent: DeviceScanFragmentEvent()
    }

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
                if(it.data is Pair<*, *>){
                   @Suppress("UNCHECKED_CAST")
                   val data : Pair<Wallet, Device> = it.data as Pair<Wallet, Device>

                    NavGraphDirections.actionGlobalLoginFragment(wallet = data.first, deviceId = data.second.id).let { navDirections ->
                        navigate(findNavController(), navDirections.actionId, navDirections.arguments, isLogout = true)
                    }
                }
            }

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