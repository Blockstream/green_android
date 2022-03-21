package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.DeviceInfoBottomSheetBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.items.NetworkSmallListItem
import com.blockstream.green.utils.navigate
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeviceInfoBottomSheetDialogFragment : BottomSheetDialogFragment(), DeviceInfoCommon{

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

        val fastItemAdapter = createNetworkAdapter(
            context = requireContext(),
            viewLifecycleOwner = viewLifecycleOwner,
            device = device,
            greenWallet = greenWallet,
            settingsManager = settingsManager
        )

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
}