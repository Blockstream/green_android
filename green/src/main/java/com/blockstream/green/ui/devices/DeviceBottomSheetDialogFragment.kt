package com.blockstream.green.ui.devices

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.DeviceBottomSheetFragmentBinding
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.utils.navigate
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeviceBottomSheetDialogFragment: BottomSheetDialogFragment() {

    private lateinit var binding: DeviceBottomSheetFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DeviceBottomSheetFragmentBinding.inflate(layoutInflater, container, false).also {
            it.lifecycleOwner = this
        }

        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Green_BottomSheetDialogTheme_Wallet)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    val args: DeviceBottomSheetDialogFragmentArgs by navArgs()

    @Inject
    lateinit var viewModelFactory: DeviceInfoViewModel.AssistedFactory
    val viewModel: DeviceInfoViewModel by viewModels {
        DeviceInfoViewModel.provideFactory(viewModelFactory,requireContext().applicationContext,  deviceManager.getDevice(args.deviceId)!!)
    }

    @Inject
    lateinit var deviceManager: DeviceManager

    @Inject
    lateinit var greenWallet: GreenWallet


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if(deviceManager.getDevice(args.deviceId) == null){
            dismiss()
            return
        }

        binding.vm = viewModel
//
//        // Device went offline
//        viewModel.onEvent.observe(viewLifecycleOwner) {
//            it.getContentIfNotHandledOrReturnNull()?.let {
//                if (it is DeviceInfoViewModel.DeviceState){
//                    // IS DISCONNECT ? check
//                      dismiss()
//                }
//            }
//        }


        binding.buttonConnect.setOnClickListener {
            DeviceBottomSheetDialogFragmentDirections.actionGlobalLoginFragment(Wallet.createEmulatedHardwareWallet(greenWallet.networks.bitcoinGreen), deviceId = args.deviceId).also {
                navigate(findNavController(), it.actionId, it.arguments, false, null)
            }

        }

        binding.buttonAuthorize.setOnClickListener {
            // viewModel.device.askForPermissionOrBond()
        }

        binding.buttonOnBoarding.setOnClickListener {
//            (requireParentFragment().requireParentFragment() as AppFragment<*>).navigate(DeviceInfoFragmentDirections.actionGlobalAddWalletFragment(deviceId = args.deviceId))
            findNavController().navigate(DeviceInfoFragmentDirections.actionGlobalAddWalletFragment(deviceId = args.deviceId))
            dismiss()
        }
    }
}