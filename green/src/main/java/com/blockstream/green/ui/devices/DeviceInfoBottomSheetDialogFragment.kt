package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.DeviceInfoBottomSheetBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.items.NetworkSmallListItem
import com.blockstream.green.ui.items.TitleExpandableListItem
import com.blockstream.green.utils.navigate
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.fastadapter.ui.utils.StringHolder
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeviceInfoBottomSheetDialogFragment : BottomSheetDialogFragment(){

    companion object{
        const val DEVICE_ID = "DEVICE_ID"

        fun create(deviceId: String): DeviceInfoBottomSheetDialogFragment =
            DeviceInfoBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(DEVICE_ID, deviceId)
                }
            }
    }

    @Inject
    lateinit var deviceManager: DeviceManager

    @Inject
    lateinit var greenWallet: GreenWallet

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var viewModelFactory: DeviceInfoViewModel.AssistedFactory

    val deviceId by lazy { arguments?.getString(DEVICE_ID) ?: "" }
    val device by lazy { deviceManager.getDevice(deviceId) }

    val viewModel: DeviceInfoViewModel by viewModels {
        DeviceInfoViewModel.provideFactory(viewModelFactory, requireContext().applicationContext, device!!)
    }

    private lateinit var binding: DeviceInfoBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DeviceInfoBottomSheetBinding.inflate(layoutInflater)
        binding.isBottomSheet = true

        if (deviceManager.getDevice(deviceId) == null) {
            dismiss()
            return binding.root
        }

        binding.vm = viewModel

        viewModel.onEvent.observe(viewLifecycleOwner) { onEvent ->
            onEvent.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                NavGraphDirections.actionGlobalLoginFragment(it.data as Wallet, deviceId = deviceId).let { navDirections ->
                    navigate(findNavController(), navDirections.actionId, navDirections.arguments, isLogout = true)
                    dismiss()
                }
            }
        }

        device?.deviceState?.observe(viewLifecycleOwner){
            // Device went offline
            if(it == Device.DeviceState.DISCONNECTED){
                dismiss()
            }
        }

        val fastItemAdapter = createNetworkAdapter()

        fastItemAdapter.onClickListener = { _, _, item: GenericItem, _ ->
            when (item) {
                is NetworkSmallListItem -> {

                    val hardwareWallet = Wallet.createEmulatedHardwareWallet(
                        greenWallet.networks.getNetworkById(item.network)
                    )

                    viewModel.changeNetwork(hardwareWallet)

                    true
                }
                else -> false
            }
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = AlphaCrossFadeAnimator()
            adapter = fastItemAdapter
        }

        return binding.root
    }

    private fun createNetworkAdapter(): FastItemAdapter<GenericItem> {
        val fastItemAdapter = FastItemAdapter<GenericItem>()
        fastItemAdapter.getExpandableExtension()

        // Listen for app settings changes to enable/disable testnet networks
        settingsManager.getApplicationSettingsLiveData().observe(viewLifecycleOwner) { applicationSettings ->
            fastItemAdapter.clear()

            fastItemAdapter.add(NetworkSmallListItem(Network.GreenMainnet, greenWallet.networks.bitcoinGreen.productName))
            if(device?.supportsLiquid == true){
                fastItemAdapter.add(NetworkSmallListItem(Network.GreenLiquid, greenWallet.networks.liquidGreen.productName))
            }

            if(applicationSettings.testnet) {
                val expandable = TitleExpandableListItem(StringHolder(R.string.id_additional_networks))

                expandable.subItems.add(
                    NetworkSmallListItem(
                        Network.GreenTestnet,
                        greenWallet.networks.testnetGreen.productName
                    )
                )

                if (device?.supportsLiquid == true) {
                    expandable.subItems.add(
                        NetworkSmallListItem(
                            Network.GreenTestnetLiquid,
                            greenWallet.networks.testnetLiquidGreen.productName
                        )
                    )
                }

                greenWallet.networks.customNetwork?.let {
                    expandable.subItems.add(
                        NetworkSmallListItem(
                            it.id,
                            it.name
                        )
                    )
                }

                fastItemAdapter.add(expandable)
            }
        }

        return fastItemAdapter
    }
}