package com.blockstream.green.ui.devices

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.DeviceBrand
import com.blockstream.base.Urls
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.DeviceListFragmentBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.items.DeviceListItem
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.utils.observeList
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject
import javax.inject.Provider


@AndroidEntryPoint
class DeviceListFragment : AppFragment<DeviceListFragmentBinding>(
    layout = R.layout.device_list_fragment,
    menuRes = 0
) {
    private val args: DeviceListFragmentArgs by navArgs()

    override val screenName = "DeviceList"

    @Inject
    lateinit var viewModelFactory: DeviceListViewModel.AssistedFactory

    val viewModel: DeviceListViewModel by viewModels {
        DeviceListViewModel.provideFactory(
            viewModelFactory,
            args.deviceBrand
        )
    }

    @Inject
    lateinit var deviceManager: DeviceManager

    @Inject
    lateinit var bluetoothAdapterProvider: Provider<BluetoothAdapter?>

    private val bluetoothAdapter get() = bluetoothAdapterProvider.get()

    override val title: String
        get() = if (args.deviceBrand != DeviceBrand.Blockstream) args.deviceBrand.brand else ""

    var requestPermission: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Nothing to do here, it's already handled by DeviceManager
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        requestPermission =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
                // Nothing to do here, it's already handled by DeviceManager
            }

        val devicesAdapter = ModelAdapter<Device, DeviceListItem>() {
            DeviceListItem(it)
        }.observeList(viewLifecycleOwner, viewModel.devices)

        val fastAdapter = FastAdapter.with(devicesAdapter)

        fastAdapter.onClickListener = { _, _, item, _ ->

            // Handle Jade as an already Bonded device
            if (item.device.hasPermissionsOrIsBonded() || item.device.handleBondingByHwwImplementation()) {
                selectDevice(item.device)
            } else {
                viewModel.askForPermissionOrBond(item.device)
            }

            true
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
            addItemDecoration(
                MaterialDividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )

            isNestedScrollingEnabled = false
        }

        binding.buttonEnableBluetooth.setOnClickListener {
            if (bluetoothAdapter?.isEnabled == false && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothAdapter?.enable()
            }
        }

        binding.buttonRequestPermission.setOnClickListener {
            // Also RxBleClient.getRecommendedScanRuntimePermissions can be used
            requestPermission.launch(BLE_LOCATION_PERMISSION)
        }

        binding.buttonEnableLocationService.setOnClickListener {
            MaterialAlertDialogBuilder(
                requireContext(),
                R.style.ThemeOverlay_Green_MaterialAlertDialog
            )
                .setMessage(R.string.id_location_services_are_disabled)
                .setPositiveButton(R.string.id_enable) { _: DialogInterface, _: Int ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton(R.string.id_cancel, null)
                .show()
        }

        binding.buttonLocationServiceMoreInfo.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.BLUETOOTH_PERMISSIONS)
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let { navigate ->
                selectDevice(navigate.data as Device)
            }
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            deviceManager.refreshDevices()
        }
    }

    override fun updateToolbar() {
        super.updateToolbar()

        if (args.deviceBrand == DeviceBrand.Blockstream) {
            toolbar.logo = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.blockstream_jade_logo
            )
            toolbar.setButton(button = getString(R.string.id_get_jade)) {
                openBrowser(settingsManager.getApplicationSettings(), Urls.JADE_STORE)
            }
        } else {
            toolbar.logo = ContextCompat.getDrawable(
                requireContext(),
                args.deviceBrand.icon
            )
            toolbar.setButton(button = getString(R.string.id_blockstream_store)) {
                openBrowser(settingsManager.getApplicationSettings(), Urls.HARDWARE_STORE)
            }
        }
    }

    private fun selectDevice(device: Device) {
        navigate(DeviceListFragmentDirections.actionDeviceListFragmentToDeviceInfoFragment(deviceId = device.id))
    }

    companion object : KLogging() {
        // NOTE: BLE_LOCATION_PERMISSION should be set to FINE for Android 10 and above, or COARSE for 9 and below
        // See: https://developer.android.com/about/versions/10/privacy/changes#location-telephony-bluetooth-wifi
        val BLE_LOCATION_PERMISSION =
            when {
                Build.VERSION.SDK_INT > Build.VERSION_CODES.R -> listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )

                Build.VERSION.SDK_INT > Build.VERSION_CODES.P -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
                else -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }.toTypedArray()
    }
}