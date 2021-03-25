package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.R
import com.blockstream.green.databinding.DeviceInfoFragmentBinding
import com.blockstream.green.devices.DeviceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeviceInfoFragment : AppFragment<DeviceInfoFragmentBinding>(
    layout = R.layout.device_info_fragment,
    menuRes = 0
) {
    val args: DeviceInfoFragmentArgs by navArgs()

    @Inject
    lateinit var viewModelFactory: DeviceInfoViewModel.AssistedFactory
    val viewModel: DeviceInfoViewModel by viewModels {
        DeviceInfoViewModel.provideFactory(viewModelFactory, deviceManager.getDevice(args.deviceId)!!)
    }

    @Inject
    lateinit var deviceManager: DeviceManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if(deviceManager.getDevice(args.deviceId) == null){
            findNavController().popBackStack()
            return
        }

        binding.vm = viewModel

        // Device went offline
        viewModel.onEvent.observe(viewLifecycleOwner) {
            it.getContentIfNotHandledOrReturnNull()?.let {
                if (it is DeviceInfoViewModel.DeviceState){
                      findNavController().popBackStack()
                }
            }
        }

        binding.buttonConnect.setOnClickListener {
            viewModel.connect()
        }

        binding.buttonOnBoarding.setOnClickListener {
            navigate(DeviceInfoFragmentDirections.actionGlobalAddWalletFragment(deviceId = args.deviceId))
        }

    }

    override fun onResume() {
        super.onResume()

        setToolbar(
            drawable = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.blockstream_jade_logo
            )
        )
    }
}