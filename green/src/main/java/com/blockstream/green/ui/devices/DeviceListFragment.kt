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
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.databinding.DeviceListFragmentBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.items.DeviceListItem
import com.blockstream.green.utils.observe
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.utils.showPopupMenu
import com.blockstream.green.utils.toast
import com.blockstream.green.utils.isDevelopmentFlavor
import com.greenaddress.Bridge
import com.greenaddress.greenbits.ui.GaActivity
import com.greenaddress.greenbits.ui.authentication.RequestLoginActivity
import com.greenaddress.greenbits.ui.hardwarewallets.DeviceSelectorActivity
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

        if(requireContext().isDevelopmentFlavor()){
            fastAdapter.onLongClickListener = { view, _, item, _ ->
                if(item.device.hasPermissionsOrIsBonded()){
                    showPopupMenu(view, R.menu.menu_emulate_antiexfil) { menu ->
                        navigateToDevice(item.device, true)
                        true
                    }
                }else{
                    toast("Give USB permissions before emulating a corrupted wallet")
                }

                true
            }
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

        if(Bridge.useGreenModule) {

            setToolbar(
                drawable = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.blockstream_jade_logo
                ), button = getString(R.string.id_get_jade)
            ) {
                openBrowser(requireContext(), Urls.JADE_STORE)
            }

        }else{
            setToolbar(title = getString(R.string.id_cable), button = getString(R.string.id_blockstream_store)) {
                openBrowser(requireContext(), Urls.HARDWARE_STORE)
            }
        }
    }

    private fun navigateToDevice(device: Device, emulateAntiExfilCorruption : Boolean = false){
        if(Bridge.useGreenModule){
            navigate(NavGraphDirections.actionGlobalDeviceBottomSheetDialogFragment(device.id))
        }else{
            val intent = Intent(requireContext(), RequestLoginActivity::class.java).also {
                if(device.isUsb){
                    it.action = GaActivity.ACTION_USB_ATTACHED
                    it.putExtra(UsbManager.EXTRA_DEVICE, device.usbDevice)

                    if(emulateAntiExfilCorruption){
                        it.putExtra(RequestLoginActivity.EMULATE_ANTI_EXFIL_CORRUPTION, true)
                    }
                }else{
                    it.action = DeviceSelectorActivity.ACTION_BLE_SELECTED
                    it.putExtra(BluetoothDevice.EXTRA_UUID, device.bleService)
                    it.putExtra(BluetoothDevice.EXTRA_DEVICE, device.bleDevice?.bluetoothDevice)
                }
            }

            startActivity(intent)
        }
    }
}