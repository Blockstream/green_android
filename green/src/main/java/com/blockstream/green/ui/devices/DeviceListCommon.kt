package com.blockstream.green.ui.devices

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.DeviceListCommonBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.items.DeviceListItem
import com.blockstream.green.utils.errorDialog
import com.blockstream.green.utils.observeList
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import mu.KLogging

interface DeviceListCommon{

    var requestPermission: ActivityResultLauncher<Array<String>>

    fun selectDevice(device: Device)

    fun init(fragment: Fragment, binding: DeviceListCommonBinding, viewModel: DeviceListViewModel, settingsManager: SettingsManager){

        requestPermission = fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            // Nothing to do here, it's already handled by DeviceManager
        }

        val devicesAdapter = ModelAdapter<Device, DeviceListItem>() {
            DeviceListItem(it)
        }.observeList(fragment.viewLifecycleOwner, viewModel.devices)

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
                DividerItemDecoration(
                    fragment.requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )

            isNestedScrollingEnabled = false
        }

        binding.buttonEnableBluetooth.setOnClickListener {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (!bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable()
            }
        }

        binding.buttonRequestPermission.setOnClickListener {
            // Also RxBleClient.getRecommendedScanRuntimePermissions can be used
            requestPermission?.launch(BLE_LOCATION_PERMISSION)
        }

        binding.buttonEnableLocationService.setOnClickListener {
            MaterialAlertDialogBuilder(
                fragment.requireContext(),
                R.style.ThemeOverlay_Green_MaterialAlertDialog
            )
                .setMessage(R.string.id_location_services_are_disabled)
                .setPositiveButton(R.string.id_enable) { _: DialogInterface, _: Int ->
                    fragment.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton(R.string.id_cancel, null)
                .show()
        }

        binding.buttonLocationServiceMoreInfo.setOnClickListener {
            fragment.openBrowser(settingsManager.getApplicationSettings(), Urls.BLUETOOTH_PERMISSIONS)
        }

        viewModel.onEvent.observe(fragment.viewLifecycleOwner) { event ->
            event.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let { navigate ->
                 selectDevice(navigate.data as Device)
            }
        }

        viewModel.onError.observe(fragment.viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                fragment.errorDialog(it)
            }
        }
    }

    companion object : KLogging() {
        // NOTE: BLE_LOCATION_PERMISSION should be set to FINE for Android 10 and above, or COARSE for 9 and below
        // See: https://developer.android.com/about/versions/10/privacy/changes#location-telephony-bluetooth-wifi
        val BLE_LOCATION_PERMISSION =
            when {
                Build.VERSION.SDK_INT > Build.VERSION_CODES.R -> listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                Build.VERSION.SDK_INT > Build.VERSION_CODES.P -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
                else -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }.toTypedArray()
    }
}