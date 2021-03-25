package com.blockstream.green.ui.devices

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.*
import com.blockstream.green.databinding.DeviceListFragmentBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.items.DeviceListItem
import com.blockstream.green.utils.observe
import com.blockstream.green.utils.openBrowser
import com.greenaddress.Bridge
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class DeviceListFragment : AppFragment<DeviceListFragmentBinding>(
    layout = R.layout.device_list_fragment,
    menuRes = 0
) {
    private val viewModel: DeviceListViewModel by viewModels()

    @Inject
    lateinit var deviceManager: DeviceManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        val devicesAdapter = ModelAdapter<Device, DeviceListItem>() {
            DeviceListItem(it)
        }.observe(viewLifecycleOwner, viewModel.devices)

        val fastAdapter = FastAdapter.with(devicesAdapter)

        fastAdapter.onClickListener = { _, _, item, position ->

            if(item.device.hasPermissionsOrIsBonded()){
                navigateToDevice(item.device)
            }else{
                viewModel.askForPermissionOrBond(item.device)
            }

            true
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            deviceManager.scanDevices()
        }

        binding.buttonEnableBluetooth.setOnClickListener {
            //Disable bluetooth
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (!mBluetoothAdapter.isEnabled) {
                mBluetoothAdapter.enable()
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                if(it is Device){
                    navigateToDevice(it)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if(Bridge.usePrototype) {

            setToolbar(
                drawable = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.blockstream_jade_logo
                ), button = getString(R.string.id_get_jade)
            ) {
                openBrowser(requireContext(), Urls.JADE_STORE)
            }

        }else{
            setToolbar(title = "USB", button = getString(R.string.id_blockstream_store)) {
                openBrowser(requireContext(), Urls.HARDWARE_STORE)
            }
        }
    }

    private fun navigateToDevice(device: Device){
        if(Bridge.usePrototype){
            navigate(NavGraphDirections.actionGlobalDeviceBottomSheetDialogFragment(device.id))
        }else{
            Bridge.v3Implementation(requireContext())
        }
    }
}