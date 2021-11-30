package com.blockstream.green.ui.devices

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.LayoutInflater
import android.view.View
import androidx.arch.core.util.Function
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.DeviceBrand
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.DeviceInfoFragmentBinding
import com.blockstream.green.databinding.PinTextDialogBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.items.NetworkListItem
import com.blockstream.green.ui.items.TitleExpandableListItem
import com.blockstream.green.utils.clearNavigationResult
import com.blockstream.green.utils.getNavigationResult
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.utils.snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greenaddress.greenbits.wallets.FirmwareUpgradeRequest
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import com.greenaddress.jade.entities.JadeVersion
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.fastadapter.ui.utils.StringHolder
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeviceInfoFragment : AppFragment<DeviceInfoFragmentBinding>(
    layout = R.layout.device_info_fragment,
    menuRes = R.menu.menu_device_jade
) {
    val args: DeviceInfoFragmentArgs by navArgs()

    val device by lazy { deviceManager.getDevice(args.deviceId) }

    @Inject
    lateinit var viewModelFactory: DeviceInfoViewModel.AssistedFactory

    val viewModel: DeviceInfoViewModel by viewModels {
        DeviceInfoViewModel.provideFactory(viewModelFactory, requireContext().applicationContext, device!!)
    }

    @Inject
    lateinit var deviceManager: DeviceManager

    @Inject
    lateinit var greenWallet: GreenWallet

    override fun getAppViewModel(): AppViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (deviceManager.getDevice(args.deviceId) == null) {
            findNavController().popBackStack()
            return
        }

        super.onViewCreated(view, savedInstanceState)

        // Get result from AddWalletFragment accepting terms
        getNavigationResult<String>()?.observe(viewLifecycleOwner) { network ->
            network?.let {
                connect(it)
                clearNavigationResult()
            }
        }

        binding.vm = viewModel

        device?.deviceState?.observe(viewLifecycleOwner){
            // Device went offline
            if(it == Device.DeviceState.DISCONNECTED){
                findNavController().popBackStack()
            }
        }

        setupDeviceInteractionEvent(viewModel.onDeviceInteractionEvent)

        viewModel.onEvent.observe(viewLifecycleOwner) { onEvent ->
            onEvent.getContentIfNotHandledForType<DeviceInfoViewModel.DeviceInfoEvent>()?.let {
                when(it){
                    is DeviceInfoViewModel.DeviceInfoEvent.DeviceReady -> {
                        viewModel.hardwareWallet?.let{ wallet ->
                            navigate(
                                DeviceInfoFragmentDirections.actionGlobalLoginFragment(wallet, deviceId = args.deviceId)
                            )
                        }
                    }
                    is DeviceInfoViewModel.DeviceInfoEvent.RequestPin -> {
                        requestPin(it.deviceBrand)
                    }
                    is DeviceInfoViewModel.DeviceInfoEvent.AskForFirmwareUpgrade -> {
                        askForFirmwareUpgrade(it.request, it.callback)
                    }
                }
            }

            onEvent.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                if(it.data == DeviceInfoViewModel.REQUIRE_REBONDING){
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.ThemeOverlay_Green_MaterialAlertDialog
                    )
                        .setTitle(R.string.id_warning)
                        .setMessage(R.string.id_the_new_firmware_requires_you_to_unpair)
                        .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        }
                        .setNegativeButton(R.string.id_cancel, null)
                        .setOnDismissListener {
                            popBackStack()
                        }
                        .show()
                }
            }

            onEvent.getContentIfNotHandledForType<NavigateEvent.NavigateBack>()?.let {
                popBackStack()
            }
        }

        viewModel.instructions.observe(viewLifecycleOwner){
            it.getContentIfNotHandledOrReturnNull()?.let{
                snackbar(it)
            }
        }

        viewModel.error.observe(viewLifecycleOwner){
            it.getContentIfNotHandledOrReturnNull()?.let{
                snackbar(it)
            }
        }

        val fastItemAdapter = createNetworkAdapter()

        fastItemAdapter.onClickListener = { _, _, item: GenericItem, _ ->
            when (item) {
                is NetworkListItem -> {

                    if (settingsManager.isDeviceTermsAccepted()){
                        connect(item.network)
                    } else {
                        navigate(DeviceInfoFragmentDirections.actionGlobalAddWalletFragment(network = item.network, deviceId = device?.id))
                    }

                    true
                }
                else -> false
            }
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastItemAdapter
        }

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsDialogFragment())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater).also {
            menu.findItem(R.id.updateFirmware).isVisible = isDevelopmentFlavor() && device?.isJade == true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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
                        viewModel.setJadeFirmwareManager(JadeFirmwareManager(viewModel, viewModel.getGreenSession()).also {
                            it.setForceFirmwareUpdate(true)
                            it.setJadeFwVersionFile(channels[i])
                        })
                        connect(Network.GreenMainnet)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        device?.deviceBrand?.let {
            if(it == DeviceBrand.Blockstream){
                setToolbar(
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.blockstream_jade_logo
                    ))
            }else{
                setToolbar(
                    title = it.brand,
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        device?.deviceBrand?.icon ?: 0
                    ))
            }
        }
    }

    private fun connect(network: String){

        // Pause BLE scanning as can make unstable the connection to a ble device
        deviceManager.pauseBluetoothScanning()

        val hardwareWallet = Wallet.createEmulatedHardwareWallet(
            greenWallet.networks.getNetworkById(network)
        )

        device?.let {
            viewModel.connectDevice(sessionManager.getHardwareSessionV3(), it, hardwareWallet)
        }
    }

    private fun createNetworkAdapter(): FastItemAdapter<GenericItem> {
        val fastItemAdapter = FastItemAdapter<GenericItem>()
        fastItemAdapter.getExpandableExtension()

        // Listen for app settings changes to enable/disable testnet networks
        settingsManager.getApplicationSettingsLiveData().observe(viewLifecycleOwner) { applicationSettings ->
            fastItemAdapter.clear()

            fastItemAdapter.add(NetworkListItem(Network.GreenMainnet, greenWallet.networks.bitcoinGreen.productName, ""))
            if(device?.supportsLiquid == true){
                fastItemAdapter.add(NetworkListItem(Network.GreenLiquid, greenWallet.networks.liquidGreen.productName, ""))
            }

            if(applicationSettings.testnet) {
                val expandable = TitleExpandableListItem(StringHolder(R.string.id_additional_networks))

                expandable.subItems.add(
                    NetworkListItem(
                        Network.GreenTestnet,
                        greenWallet.networks.testnetGreen.productName,
                        ""
                    )
                )

                if (device?.supportsLiquid == true) {
                    expandable.subItems.add(
                        NetworkListItem(
                            Network.GreenTestnetLiquid,
                            greenWallet.networks.testnetLiquidGreen.productName,
                            ""
                        )
                    )
                }

                greenWallet.networks.customNetwork?.let {
                    expandable.subItems.add(
                        NetworkListItem(
                            it.id,
                            it.name,
                            "Force usage of custom network. Multisig/Singlesig selection is irrelevant."
                        )
                    )
                }

                fastItemAdapter.add(expandable)
            }
        }

        return fastItemAdapter
    }

    private fun requestPin(deviceBrand: DeviceBrand){
        val dialogBinding = PinTextDialogBinding.inflate(LayoutInflater.from(context))

        dialogBinding.hint = getString(R.string.id_pin)
        dialogBinding.message = getString(R.string.id_enter_the_pin_for_your_hardware)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.id_pin)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.requestPinEmitter?.onSuccess(dialogBinding.pin)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener {
                viewModel.requestPinEmitter?.tryOnError(Exception("id_action_canceled"))
            }
            .show()
    }

    private fun askForFirmwareUpgrade(
        request: FirmwareUpgradeRequest,
        callback: Function<Int?, Void>
    ) {
        if (request.deviceBrand == DeviceBrand.Blockstream) {
            val usbIsRequired = !request.isUsb && request.hardwareVersion == "JADE_V1.1" && JadeVersion(request.currentVersion) < JadeVersion("0.1.28")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(if (request.isUpgradeRequired) R.string.id_new_jade_firmware_required else R.string.id_new_jade_firmware_available)
                .setCancelable(false)
                .apply {
                    if(usbIsRequired){
                        setMessage(R.string.id_connect_jade_with_a_usb_cable)
                        setPositiveButton(R.string.id_read_more) { _, _ ->
                            openBrowser(Urls.HELP_JADE_USB_UPGRADE)
                            callback.apply(null)
                        }
                    }else{
                        if(request.firmwareList != null){
                            setTitle("Select firmware")
                            setItems(request.firmwareList!!.toTypedArray()){ _: DialogInterface, i: Int ->
                                callback.apply(i)
                            }
                        }else {
                            setMessage(getString(R.string.id_install_version_s, request.upgradeVersion))
                            setPositiveButton(R.string.id_continue) { _, _ ->
                                callback.apply(0)
                            }
                        }
                    }
                    if (request.isUpgradeRequired) {
                        setNegativeButton(R.string.id_cancel) { _, _ ->
                            callback.apply(null)
                        }
                    } else {
                        setNeutralButton(R.string.id_skip) { _, _ ->
                            callback.apply(null)
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
                        callback.apply(0)
                    }
                    .setNegativeButton(R.string.id_cancel) { _, _ ->
                        callback.apply(null)
                    }
                    .show()
            }

            // Send error?
        }
    }
}
