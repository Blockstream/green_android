package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.DeviceManagerAndroid
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.devices.DeviceInfoViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.devices.DeviceInfoScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class DeviceInfoFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = R.menu.menu_device_jade
) {
    val args: DeviceInfoFragmentArgs by navArgs()

    val viewModel: DeviceInfoViewModel by viewModel {
        parametersOf(args.deviceId)
    }

    override val useCompose: Boolean = true

    override fun getGreenViewModel(): GreenViewModel = viewModel

    private val deviceManager: DeviceManagerAndroid by inject()
    private val deviceOrNull by lazy { deviceManager.getDevice(args.deviceId) }
    private val device get() = deviceOrNull!!

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    DeviceInfoScreen(viewModel = viewModel)
                }
            }
        }

        viewModel.navData.onEach {
            setToolbarVisibility(it.isVisible)
            onBackCallback.isEnabled = !it.isVisible
            (requireActivity() as MainActivity).lockDrawer(!it.isVisible)
        }.launchIn(lifecycleScope)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

         if (sideEffect is SideEffects.OpenDialog) {
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

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.updateFirmware).isVisible = isDevelopmentFlavor && device.isJade == true
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.updateFirmware -> {
                viewModel.postEvent(Events.EventSideEffect(sideEffect = DeviceInfoViewModel.LocalSideEffects.SelectFirmwareChannel()))
            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    override fun updateToolbar() {
        super.updateToolbar()

        if(deviceOrNull?.deviceBrand == DeviceBrand.Blockstream){
            toolbar.setButton(button = getString(R.string.id_setup_guide)) {
                navigate(DeviceInfoFragmentDirections.actionDeviceInfoFragmentToJadeGuideFragment())
            }
        }
    }

    companion object: Loggable()
}
