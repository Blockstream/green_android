package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import com.blockstream.common.data.ScanResult
import com.blockstream.common.data.ScreenLockSetting
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.settings.AppSettingsViewModel
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract.Companion.DEFAULT_BITCOIN_ELECTRUM_URL
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract.Companion.DEFAULT_LIQUID_ELECTRUM_URL
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract.Companion.DEFAULT_MULTI_SPV_BITCOIN_URL
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract.Companion.DEFAULT_MULTI_SPV_LIQUID_URL
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract.Companion.DEFAULT_MULTI_SPV_TESTNET_LIQUID_URL
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract.Companion.DEFAULT_MULTI_SPV_TESTNET_URL
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract.Companion.DEFAULT_TESTNET_ELECTRUM_URL
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract.Companion.DEFAULT_TESTNET_LIQUID_ELECTRUM_URL
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.green.R
import com.blockstream.green.databinding.AppSettingsFragmentBinding
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.stringFromIdentifier
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.ConsentBottomSheetDialogFragment
import com.blockstream.green.utils.isDevelopmentFlavor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel


class AppSettingsFragment : AppFragment<AppSettingsFragmentBinding>(R.layout.app_settings_fragment, R.menu.app_settings) {
    private val viewModel: AppSettingsViewModel by viewModel()

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel() = null

    private val onBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(viewModel.areSettingsDirty()){
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.id_app_settings)
                    .setMessage(R.string.id_your_settings_are_unsavednndo)
                    .setPositiveButton(R.string.id_continue) { _, _ ->
                        isEnabled = false
                        popBackStack()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }else{
                popBackStack()
            }
        }
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if (sideEffect is AppSettingsViewModel.LocalSideEffects.AnalyticsMoreInfo) {
            ConsentBottomSheetDialogFragment.show(childFragmentManager, hideButtons = true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<ScanResult>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(
            viewLifecycleOwner
        ) { result ->
            result?.also { scannedCode ->
                viewModel.postEvent(AppSettingsViewModel.LocalEvents.InvitationCode(scannedCode.result))
            }
        }

        binding.vm = viewModel
        binding.isDevelopment = isDevelopmentFlavor
        val screenLockSettings = ScreenLockSetting.getStringList().map {
            requireContext().stringFromIdentifier(it)
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, screenLockSettings)
        binding.screenLockSetting.setAdapter(adapter)
        binding.screenLockSetting.setText(screenLockSettings[viewModel.screenLockInSeconds.value.ordinal], false)
        binding.screenLockSetting.setOnItemClickListener { _, _, position, _ ->
            ScreenLockSetting.byPosition(position).let {
                viewModel.screenLockInSeconds.value = it
            }
        }

        binding.bitcoinElectrumServerPlaceholder = DEFAULT_BITCOIN_ELECTRUM_URL
        binding.liquidElectrumServerPlaceholder = DEFAULT_LIQUID_ELECTRUM_URL
        binding.testnetElectrumServerPlaceholder = DEFAULT_TESTNET_ELECTRUM_URL
        binding.testnetLiquidElectrumServerPlaceholder = DEFAULT_TESTNET_LIQUID_ELECTRUM_URL

        binding.bitcoinSpvElectrumServerPlaceholder = DEFAULT_MULTI_SPV_BITCOIN_URL
        binding.liquidSpvElectrumServerPlaceholder = DEFAULT_MULTI_SPV_LIQUID_URL
        binding.testnetSpvElectrumServerPlaceholder = DEFAULT_MULTI_SPV_TESTNET_URL
        binding.testnetLiquidSpvElectrumServerPlaceholder = DEFAULT_MULTI_SPV_TESTNET_LIQUID_URL

        binding.buttonAnalyticsMoreInfo.setOnClickListener {
            viewModel.postEvent(AppSettingsViewModel.LocalEvents.AnalyticsMoreInfo)
        }

        binding.personalBitcoinElectrumServerInputLayout.endIconCustomMode()
        binding.personalLiquidElectrumServerInputLayout.endIconCustomMode()
        binding.personalTestnetElectrumServerInputLayout.endIconCustomMode()
        binding.personalTestnetLiquidElectrumServerInputLayout.endIconCustomMode()
        binding.proxyURLInputLayout.endIconCustomMode()
        binding.spvBitcoinElectrumServerInputLayout.endIconCustomMode()
        binding.spvLiquidElectrumServerInputLayout.endIconCustomMode()
        binding.spvTestnetElectrumServerInputLayout.endIconCustomMode()
        binding.spvTestnetLiquidElectrumServerInputLayout.endIconCustomMode()

        binding.buttonSave.setOnClickListener {
            viewModel.postEvent(AppSettingsViewModel.LocalEvents.Save)
        }

        binding.buttonCancel.setOnClickListener {
            popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.buttonInvitation -> {

                val dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(context))
                dialogBinding.textInputLayout.endIconCustomMode()

                dialogBinding.hint = getString(R.string.id_enter_your_code)
                dialogBinding.text = ""

                MaterialAlertDialogBuilder(requireContext())
                    .setIcon(R.drawable.ic_fill_flask_24)
                    .setTitle(R.string.id_enable_experimental_features)
                    .setView(dialogBinding.root)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.postEvent(AppSettingsViewModel.LocalEvents.InvitationCode(dialogBinding.text.toString()))
                    }
                    .setNeutralButton(R.string.id_qr_code) { _, _ ->
                        CameraBottomSheetDialogFragment.showSingle(
                            screenName = screenName,
                            fragmentManager = childFragmentManager
                        )
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }
            else -> super.onMenuItemSelected(menuItem)
        }
    }

    fun navigateUp(){
        onBackCallback.handleOnBackPressed()
    }
}
