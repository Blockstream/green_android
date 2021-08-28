package com.blockstream.green.ui.devices

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.DeviceInfoFragmentBinding
import com.blockstream.green.databinding.PinTextDialogBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.items.NetworkListItem
import com.blockstream.green.ui.items.TitleExpandableListItem
import com.blockstream.green.utils.clearNavigationResult
import com.blockstream.green.utils.getNavigationResult
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greenaddress.greenbits.ui.GaActivity
import com.greenaddress.greenbits.ui.authentication.TrezorPassphraseActivity
import com.greenaddress.greenbits.ui.authentication.TrezorPinActivity
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.fastadapter.ui.utils.StringHolder
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import io.reactivex.SingleEmitter
import javax.inject.Inject

@AndroidEntryPoint
class DeviceInfoFragment : AppFragment<DeviceInfoFragmentBinding>(
    layout = R.layout.device_info_fragment,
    menuRes = 0
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

    private val startForResultPinMatrix = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            viewModel.requestPinMatrixEmitter?.onSuccess(intent?.getStringExtra(GaActivity.HARDWARE_PIN_REQUEST.toString()))
        }else{
            viewModel.requestPinMatrixEmitter?.onError(Exception("id_action_canceled"))
        }
    }

    private val startForResultPassphrase = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            viewModel.requestPinPassphraseEmitter?.onSuccess(intent?.getStringExtra(GaActivity.HARDWARE_PASSPHRASE_REQUEST.toString()))
        }else{
            viewModel.requestPinPassphraseEmitter?.onError(Exception("id_action_canceled"))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (deviceManager.getDevice(args.deviceId) == null) {
            findNavController().popBackStack()
            return
        }

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
            onEvent.getContentIfNotHandledOrReturnNull()?.let {
                if(it is DeviceInfoViewModel.Event){
                    when(it){
                        is DeviceInfoViewModel.Event.DeviceReady -> {
                            viewModel.hardwareWallet?.let{ wallet ->
                                navigate(
                                    DeviceInfoFragmentDirections.actionGlobalLoginFragment(wallet, deviceId = args.deviceId)
                                )
                            }
                        }
                        is DeviceInfoViewModel.Event.RequestPin -> {
                            requestPin(it.deviceBrand)
                        }
                        is DeviceInfoViewModel.Event.AskForFirmwareUpgrade -> {
                            askForFirmwareUpgrade(it.deviceBrand, it.version, it.upgradeRequired, it.callback)
                        }
                        is DeviceInfoViewModel.Event.RequestPinMatrix -> {
                            requestPinMatrix()
                        }
                        is DeviceInfoViewModel.Event.RequestPassphrase -> {
                            requestPassphrase()
                        }
                    }
                }
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

        binding.buttonConnectionSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalConnectionSettingsDialogFragment())
        }
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

        fastItemAdapter.add(NetworkListItem(Network.GreenMainnet, greenWallet.networks.bitcoinGreen.productName, ""))
        if(device?.supportsLiquid == true){
            fastItemAdapter.add(NetworkListItem(Network.GreenLiquid, greenWallet.networks.liquidGreen.productName, ""))
        }

        val expandable = TitleExpandableListItem(StringHolder(R.string.id_additional_networks))
        expandable.subItems.add(NetworkListItem(Network.GreenTestnet, greenWallet.networks.testnetGreen.productName, ""))

        if (isDevelopmentFlavor()) {
            if(device?.supportsLiquid == true) {
                expandable.subItems.add(
                    NetworkListItem(
                        Network.GreenTestnetLiquid,
                        greenWallet.networks.testnetLiquidGreen.productName,
                        ""
                    )
                )
            }
        }

        fastItemAdapter.add(expandable)

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
        deviceBrand: DeviceBrand,
        version: String?,
        isUpgradeRequired: Boolean,
        callback: Function<Boolean?, Void?>?
    ) {
        if (deviceBrand == DeviceBrand.Blockstream) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(if (isUpgradeRequired) R.string.id_new_jade_firmware_required else R.string.id_new_jade_firmware_available)
                .setMessage(getString(R.string.id_install_version_s, version))
                .setPositiveButton(R.string.id_continue) { _, _ ->
                    callback?.apply(true)
                }.also {
                    if (isUpgradeRequired) {
                        it.setNegativeButton(R.string.id_cancel) { _, _ ->
                            callback?.apply(false)
                        }
                    } else {
                        it.setNeutralButton(R.string.id_skip) { _, _ ->
                            callback?.apply(false)
                        }
                    }
                }
                .show()
        } else {
            snackbar(R.string.id_outdated_hardware_wallet)

            if (!isUpgradeRequired) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.id_warning)
                    .setMessage(R.string.id_outdated_hardware_wallet)
                    .setPositiveButton(R.string.id_continue) { _, _ ->
                        callback?.apply(true)
                    }
                    .setNegativeButton(R.string.id_cancel) { _, _ ->
                        callback?.apply(false)
                    }
                    .show()
            }

            // Send error?
        }
    }

    private fun requestPinMatrix() {
        startForResultPinMatrix.launch(Intent(requireContext(), TrezorPinActivity::class.java))
    }

    private fun requestPassphrase() {
        startForResultPassphrase.launch(Intent(requireContext(), TrezorPassphraseActivity::class.java))
    }
}