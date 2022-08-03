package com.blockstream.green.ui.devices

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.arch.core.util.Function
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.DeviceBrand
import com.blockstream.base.Urls
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.Network
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.DeviceInfoFragmentBinding
import com.blockstream.green.databinding.PinTextDialogBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.navigate
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.bottomsheets.MenuBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.MenuDataProvider
import com.blockstream.green.ui.items.MenuListItem
import com.blockstream.green.ui.items.NetworkSmallListItem
import com.blockstream.green.ui.items.TitleExpandableListItem
import com.blockstream.green.ui.settings.AppSettingsDialogFragment
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greenaddress.greenbits.wallets.FirmwareUpgradeRequest
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import com.greenaddress.jade.entities.JadeVersion
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeviceInfoFragment : AppFragment<DeviceInfoFragmentBinding>(
    layout = R.layout.device_info_fragment,
    menuRes = R.menu.menu_device_jade
), MenuDataProvider {
    val args: DeviceInfoFragmentArgs by navArgs()

    val device by lazy { deviceManager.getDevice(args.deviceId) }

    override val screenName = "DeviceInfo"

    @Inject
    lateinit var viewModelFactory: DeviceInfoViewModel.AssistedFactory

    val viewModel: DeviceInfoViewModel by viewModels {
        DeviceInfoViewModel.provideFactory(viewModelFactory, requireContext().applicationContext, device!!, args.wallet)
    }

    @Inject
    lateinit var deviceManager: DeviceManager

    @Inject
    lateinit var gdkBridge: GdkBridge

    override val title: String?
        get() = if(device?.deviceBrand == DeviceBrand.Blockstream) "" else device?.deviceBrand?.name

    override fun getAppViewModel(): AppViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (device == null) {
            snackbar("Device not longer available")
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

        viewModel.deviceState.observe(viewLifecycleOwner){
            // Device went offline
            if(it == Device.DeviceState.DISCONNECTED){
                snackbar("Device not longer available")
                findNavController().popBackStack()
            }
        }

        setupDeviceInteractionEvent(viewModel.onDeviceInteractionEvent)

        viewModel.onEvent.observe(viewLifecycleOwner) { onEvent ->
            onEvent.getContentIfNotHandledForType<DeviceInfoViewModel.DeviceInfoEvent>()?.let {
                when(it){
                    is DeviceInfoViewModel.DeviceInfoEvent.RequestPin -> {
                        requestPin()
                    }
                    is DeviceInfoViewModel.DeviceInfoEvent.AskForFirmwareUpgrade -> {
                        askForFirmwareUpgrade(it.request, it.callback)
                    }
                }
            }

            onEvent.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                if(it.data is Wallet){
                    NavGraphDirections.actionGlobalLoginFragment(it.data, deviceId = device?.id).let { navDirections ->
                        navigate(findNavController(), navDirections.actionId, navDirections.arguments, isLogout = true)
                    }
                } else if(it.data == DeviceInfoViewModel.REQUIRE_REBONDING){
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

        val fastItemAdapter = createNetworkAdapter(
            context = requireContext(),
            viewLifecycleOwner = viewLifecycleOwner,
            device = device,
            gdkBridge = gdkBridge,
            settingsManager = settingsManager
        )

        fastItemAdapter.onClickListener = { _, _, item: GenericItem, _ ->
            when (item) {
                is NetworkSmallListItem -> {
                    connect(item.network)
                    true
                }
                else -> false
            }
        }

        binding.buttonLoginWithDevice.setOnClickListener {
            MenuBottomSheetDialogFragment.show(
                title = getString(R.string.id_select_network), menuItems = listOf(
                    MenuListItem(
                        icon = R.drawable.ic_regular_currency_btc_24,
                        title = StringHolder("Mainnet")
                    ),
                    MenuListItem(
                        icon = R.drawable.ic_regular_flask_24,
                        title = StringHolder("Testnet")
                    )
                ), fragmentManager = childFragmentManager
            )
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastItemAdapter
        }

        binding.buttonAppSettings.setOnClickListener {
            AppSettingsDialogFragment.show(childFragmentManager)
        }
    }

    override fun menuItemClicked(requestCode: Int, item: GenericItem, position: Int) {
        connect((if (position == 0) gdkBridge.networks.bitcoinElectrum else gdkBridge.networks.testnetBitcoinElectrum).id)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.updateFirmware).isVisible = isDevelopmentFlavor && device?.isJade == true
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
                        viewModel.setJadeFirmwareManager(
                            JadeFirmwareManager(
                                viewModel,
                                viewModel.getGreenSession(),
                                channels[i],
                                true
                            ))
                        connect(Network.GreenMainnet)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    override fun updateToolbar() {
        super.updateToolbar()

        device?.deviceBrand?.let {
            if(it == DeviceBrand.Blockstream){
                toolbar.logo = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.blockstream_jade_logo
                )
            }else{
                toolbar.logo = ContextCompat.getDrawable(
                    requireContext(),
                    it.icon
                )
            }
        }
    }

    private fun connect(network: String){
        if (settingsManager.isDeviceTermsAccepted()){

            device?.let {
                viewModel.connectDeviceToNetwork(network)
            }
        } else {
            navigate(DeviceInfoFragmentDirections.actionGlobalAddWalletFragment(network = network, deviceId = device?.id))
        }
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

    private fun createNetworkAdapter(
        context: Context,
        viewLifecycleOwner: LifecycleOwner,
        device: Device?,
        gdkBridge: GdkBridge,
        settingsManager: SettingsManager
    ): FastItemAdapter<GenericItem> {
        val fastItemAdapter = FastItemAdapter<GenericItem>()
        fastItemAdapter.getExpandableExtension()

        // Listen for app settings changes to enable/disable testnet networks
        settingsManager.getApplicationSettingsLiveData().observe(viewLifecycleOwner) { applicationSettings ->
            fastItemAdapter.clear()

            fastItemAdapter.add(NetworkSmallListItem(Network.ElectrumMainnet, gdkBridge.networks.bitcoinElectrum.canonicalName))

            if(device?.supportsLiquid == true){
                fastItemAdapter.add(NetworkSmallListItem(Network.GreenLiquid, gdkBridge.networks.liquidGreen.canonicalName))
            }

            if(applicationSettings.testnet) {
                val expandable = TitleExpandableListItem(StringHolder(R.string.id_additional_networks))

                expandable.subItems.add(
                    NetworkSmallListItem(
                        Network.ElectrumTestnet,
                        gdkBridge.networks.testnetBitcoinElectrum.canonicalName
                    )
                )

                if(device?.supportsLiquid == true){
                    expandable.subItems.add(
                        NetworkSmallListItem(
                            Network.GreenTestnetLiquid,
                            gdkBridge.networks.testnetLiquidGreen.canonicalName
                        )
                    )
                }

                gdkBridge.networks.customNetwork?.let {
                    expandable.subItems.add(
                        NetworkSmallListItem(
                            it.id,
                            it.name
                        )
                    )
                }

                fastItemAdapter.add(expandable)
            }
        }

        return fastItemAdapter
    }
}
