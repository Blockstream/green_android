package com.blockstream.green.ui.devices

import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.core.app.ActivityCompat
import androidx.databinding.ViewDataBinding
import com.blockstream.common.Urls
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.green.R
import com.blockstream.green.databinding.PinTextDialogBinding
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.EnvironmentBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.JadeFirmwareUpdateBottomSheetDialogFragment
import com.blockstream.green.utils.openBrowser
import com.blockstream.jade.data.JadeVersion
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greenaddress.greenbits.wallets.FirmwareUpgradeRequest
import org.koin.android.ext.android.inject


abstract class AbstractDeviceFragment<T : ViewDataBinding>(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    private val bluetoothManager: BluetoothManager by inject()
    private val bluetoothAdapter: BluetoothAdapter? by inject()

    abstract val viewModel: AbstractDeviceViewModel

    private val requestPermission: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            // tell bluetooth manager to recheck permissions
            bluetoothManager.permissionsGranted()
        }
    }

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        when (sideEffect) {
            is AbstractDeviceViewModel.LocalSideEffects.RequestPin -> {
                requestPin()
            }
            is AbstractDeviceViewModel.LocalSideEffects.RequestNetwork -> {
                requestNetwork()
            }
            is AbstractDeviceViewModel.LocalSideEffects.AskForFirmwareUpgrade -> {
                askForFirmwareUpgrade(sideEffect.request)
            }
            is AbstractDeviceViewModel.LocalSideEffects.FirmwarePushedToDevice -> {
                JadeFirmwareUpdateBottomSheetDialogFragment.show(childFragmentManager)
                viewModel.firmwareState.value = sideEffect
            }
            is AbstractDeviceViewModel.LocalSideEffects.FirmwareUpdateComplete,
            is AbstractDeviceViewModel.LocalSideEffects.FirmwareUpdateProgress -> {
                viewModel.firmwareState.value = sideEffect
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.onInstructions.observe(viewLifecycleOwner){
            it.getContentIfNotHandledOrReturnNull()?.also{ res ->
                snackbar(res)
            }
        }
    }

    protected fun requestPermissions(){
        requestPermission.launch(BluetoothManager.BLE_PERMISSIONS)
    }

    protected fun enableBluetooth(){
        if(bluetoothAdapter?.isEnabled == false){
            if(BluetoothManager.BLE_PERMISSIONS.all { ActivityCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }){
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    bluetoothAdapter?.enable()
                } else {
                    startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            } else {
                requestPermissions()
            }
        }
    }

    protected fun enableLocationService(){
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

    private fun requestPin() {
        val dialogBinding = PinTextDialogBinding.inflate(LayoutInflater.from(context))

        dialogBinding.hint = getString(R.string.id_pin)
        dialogBinding.message = getString(R.string.id_enter_the_pin_for_your_hardware)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.id_pin)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.requestPinEmitter?.complete(dialogBinding.pin ?: "")
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener {
                viewModel.requestPinEmitter?.completeExceptionally(Exception("id_action_canceled"))
            }
            .show()
    }

    open fun requestNetwork() {
        if(!settingsManager.getApplicationSettings().testnet){
            viewModel.requestNetworkEmitter?.complete(gdk.networks().bitcoinElectrum)
        }else {
            EnvironmentBottomSheetDialogFragment.show(fragmentManager = childFragmentManager)
        }
    }

    private fun askForFirmwareUpgrade(
        request: FirmwareUpgradeRequest
    ) {
        viewModel.askForFirmwareUpgradeEmitter?.also { askForFirmwareUpgradeEmitter ->
            if (request.deviceBrand == DeviceBrand.Blockstream) {
                val usbIsRequired = !request.isUsb && request.hardwareVersion == "JADE_V1.1" && JadeVersion(request.currentVersion ?: "") < JadeVersion("0.1.28")
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(if (request.isUpgradeRequired) R.string.id_new_jade_firmware_required else R.string.id_new_jade_firmware_available)
                    .setCancelable(false)
                    .apply {
                        if(usbIsRequired){
                            setMessage(R.string.id_connect_jade_with_a_usb_cable)
                            setPositiveButton(R.string.id_read_more) { _, _ ->
                                openBrowser(Urls.HELP_JADE_USB_UPGRADE)
                                askForFirmwareUpgradeEmitter.complete(null)
                            }
                        }else{
                            if(request.firmwareList != null){
                                setTitle("Select firmware")
                                setItems(request.firmwareList!!.toTypedArray()){ _: DialogInterface, i: Int ->
                                    askForFirmwareUpgradeEmitter.complete(i)
                                }
                            }else {
                                setMessage(getString(R.string.id_install_version_s, request.upgradeVersion))
                                setPositiveButton(R.string.id_continue) { _, _ ->
                                    askForFirmwareUpgradeEmitter.complete(0)
                                }
                            }
                        }
                        if (request.isUpgradeRequired) {
                            setNegativeButton(R.string.id_cancel) { _, _ ->
                                askForFirmwareUpgradeEmitter.complete(null)
                            }
                        } else {
                            setNeutralButton(R.string.id_skip) { _, _ ->
                                askForFirmwareUpgradeEmitter.complete(null)
                            }
                        }
                    }
                    .show()
            } else {
                snackbar(R.string.id_outdated_hardware_wallet)

                if (!request.isUpgradeRequired) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.id_warning)
                        .setMessage(R.string.id_outdated_hardware_wallet)
                        .setPositiveButton(R.string.id_continue) { _, _ ->
                            askForFirmwareUpgradeEmitter.complete(0)
                        }
                        .setNegativeButton(R.string.id_cancel) { _, _ ->
                            askForFirmwareUpgradeEmitter.complete(null)
                        }
                        .show()
                }

                // Send error?
            }
        }
    }
}