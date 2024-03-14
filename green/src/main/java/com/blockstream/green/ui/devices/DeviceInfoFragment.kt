package com.blockstream.green.ui.devices

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.DeviceBrand
import com.blockstream.common.gdk.device.DeviceState
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.DeviceInfoFragmentBinding
import com.blockstream.green.devices.DeviceManagerAndroid
import com.blockstream.green.extensions.navigate
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.ui.bottomsheets.EnvironmentListener
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import mu.KLogging
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class DeviceInfoFragment : AbstractDeviceFragment<DeviceInfoFragmentBinding>(
    layout = R.layout.device_info_fragment,
    menuRes = R.menu.menu_device_jade
), EnvironmentListener {
    val args: DeviceInfoFragmentArgs by navArgs()

    private val deviceOrNull by lazy { deviceManager.getAndroidDevice(args.deviceId) }
    private val device get() = deviceOrNull!!

    override val screenName = "DeviceInfo"

    override val viewModel: DeviceInfoViewModel by viewModel {
        parametersOf(device)
    }

    private val deviceManager: DeviceManagerAndroid by inject()

    override val title: String?
        get() = deviceOrNull?.deviceBrand?.name

    override fun getGreenViewModel(): GreenViewModel? = if(deviceOrNull == null) null else viewModel

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {

        }
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if (sideEffect is SideEffects.Navigate){
            (sideEffect.data as? GreenWallet)?.also {
                NavGraphDirections.actionGlobalLoginFragment(it, deviceId = device.connectionIdentifier)
                    .let { navDirections ->
                        navigate(
                            findNavController(),
                            navDirections.actionId,
                            navDirections.arguments,
                            isLogout = true
                        )
                    }
            }

            if(sideEffect.data == DeviceInfoViewModel.REQUIRE_REBONDING){
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.ThemeOverlay_Green_MaterialAlertDialog
                )
                    .setTitle(R.string.id_warning)
                    .setMessage(R.string.id_the_new_firmware_requires_you)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }
                    .setNegativeButton(R.string.id_cancel, null)
                    .setOnDismissListener {
                        popBackStack()
                    }
                    .show()
            }
        } else if (sideEffect is SideEffects.OpenDialog) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.id_new_jade_firmware_required )
                .setCancelable(false)
                .setMessage(R.string.id_please_upgrade_your_jade_firmware_to)
                .setPositiveButton(R.string.id_ok) { _, _ ->
                    popBackStack()
                }
                .setNeutralButton(R.string.id_read_more) { _, _ ->
                    popBackStack()
                    openBrowser(Urls.HELP_JADE_USB_UPGRADE)
                }.show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (deviceOrNull == null) {
            snackbar("Device not longer available")
            findNavController().popBackStack()
            return
        }

        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        viewModel.navigationLock.observe(viewLifecycleOwner){
            onBackCallback.isEnabled = it
            (requireActivity() as MainActivity).lockDrawer(it)
        }

        viewModel.deviceState.observe(viewLifecycleOwner) {
            // Device went offline
            if (it == DeviceState.DISCONNECTED) {
                snackbar("Device is disconnected")
                findNavController().popBackStack()
            }
        }

        binding.buttonTroubleshoot.setOnClickListener {
            openBrowser(Urls.JADE_TROUBLESHOOT)
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.authenticateAndContinue()
        }

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.updateFirmware).isVisible = isDevelopmentFlavor && device.isJade == true
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.updateFirmware -> {

                val channels = listOf(
                    JadeFirmwareManager.JADE_FW_VERSIONS_BETA,
                    JadeFirmwareManager.JADE_FW_VERSIONS_LATEST,
                    JadeFirmwareManager.JADE_FW_VERSIONS_PREVIOUS
                )

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Firmware Channel")
                    .setItems(channels.toTypedArray()){ _: DialogInterface, i: Int ->
                        // Update
                        viewModel.authenticateAndContinue(jadeFirmwareManager = JadeFirmwareManager(
                            viewModel,
                            sessionManager.httpRequestProvider,
                            channels[i],
                            true
                        ))
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    override fun onResume() {
        super.onResume()

        if(deviceOrNull == null){
            popBackStack()
        }
    }

    override fun updateToolbar() {
        super.updateToolbar()

        if(deviceOrNull?.deviceBrand == DeviceBrand.Blockstream){
            toolbar.setButton(button = getString(R.string.id_setup_guide)) {
                navigate(DeviceInfoFragmentDirections.actionDeviceInfoFragmentToJadeGuideFragment())
            }
        }
    }

    override fun onEnvironmentSelected(isTestnet: Boolean?, customNetwork: Network?) {
        if(isTestnet == null){
            viewModel.requestNetworkEmitter?.completeExceptionally(Exception("id_action_canceled"))
        }else{
            viewModel.requestNetworkEmitter?.also {
                it.complete(if(isTestnet) gdk.networks().testnetBitcoinElectrum else gdk.networks().bitcoinElectrum)
            }
        }
    }

    companion object: KLogging()
}
