package com.blockstream.green.ui.devices

import android.view.LayoutInflater
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.models.devices.AbstractDeviceViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.PinTextDialogBinding
import com.blockstream.green.ui.AppFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.android.ext.android.inject


abstract class AbstractDeviceFragment<T : ViewDataBinding>(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    private val bluetoothManager: BluetoothManager by inject()

    abstract val viewModel: AbstractDeviceViewModel

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


//    private fun askForFirmwareUpgrade(
//        request: FirmwareUpgradeRequest
//    ) {
//        viewModel.askForFirmwareUpgradeEmitter?.also { askForFirmwareUpgradeEmitter ->
//            if (request.deviceBrand == DeviceBrand.Blockstream.brand) {
////                val usbIsRequired = !request.isUsb && request.hardwareVersion == "JADE_V1.1" && JadeVersion(request.currentVersion ?: "") < JadeVersion("0.1.28")
//                MaterialAlertDialogBuilder(requireContext())
//                    .setTitle(if (request.isUpgradeRequired) R.string.id_new_jade_firmware_required else R.string.id_new_jade_firmware_available)
//                    .setCancelable(false)
//                    .apply {
////                        if(usbIsRequired){
////                            setMessage(R.string.id_connect_jade_with_a_usb_cable)
////                            setPositiveButton(R.string.id_read_more) { _, _ ->
////                                openBrowser(Urls.HELP_JADE_USB_UPGRADE)
////                                askForFirmwareUpgradeEmitter.complete(null)
////                            }
////                        }else{
//                            if(request.firmwareList != null){
//                                setTitle("Select firmware")
//                                setItems(request.firmwareList!!.toTypedArray()){ _: DialogInterface, i: Int ->
//                                    askForFirmwareUpgradeEmitter.complete(i)
//                                }
//                            }else {
//                                setMessage(getString(R.string.id_install_version_s, request.upgradeVersion))
//                                setPositiveButton(R.string.id_continue) { _, _ ->
//                                    askForFirmwareUpgradeEmitter.complete(0)
//                                }
//                            }
////                        }
//                        if (request.isUpgradeRequired) {
//                            setNegativeButton(R.string.id_cancel) { _, _ ->
//                                askForFirmwareUpgradeEmitter.complete(null)
//                            }
//                        } else {
//                            setNeutralButton(R.string.id_skip) { _, _ ->
//                                askForFirmwareUpgradeEmitter.complete(null)
//                            }
//                        }
//                    }
//                    .show()
//            } else {
//
//                if (!request.isUpgradeRequired) {
//                    MaterialAlertDialogBuilder(requireContext())
//                        .setTitle(R.string.id_warning)
//                        .setMessage(R.string.id_outdated_hardware_wallet)
//                        .setPositiveButton(R.string.id_continue) { _, _ ->
//                            askForFirmwareUpgradeEmitter.complete(0)
//                        }
//                        .setNegativeButton(R.string.id_cancel) { _, _ ->
//                            askForFirmwareUpgradeEmitter.complete(null)
//                        }
//                        .show()
//                }
//
//                // Send error?
//            }
//        }
//    }
}