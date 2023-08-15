package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import com.blockstream.common.data.ScreenLockSetting
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive


@AndroidEntryPoint
class AppSettingsFragment : AppFragment<AppSettingsFragmentBinding>(R.layout.app_settings_fragment, R.menu.app_settings) {
    override val screenName = "AppSettings"

    private val viewModel: AppSettingsViewModel by viewModels()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(
            viewLifecycleOwner
        ) { result ->
            result?.also { scannedCode ->
                checkInvitationCode(scannedCode)
            }
        }

        binding.vm = viewModel
        binding.isDevelopment = isDevelopmentFlavor
        val screenLockSettings = ScreenLockSetting.getStringList().map {
            requireContext().stringFromIdentifier(it)
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, screenLockSettings)
        binding.screenLockSetting.setAdapter(adapter)
        binding.screenLockSetting.setText(screenLockSettings[ScreenLockSetting.bySeconds(viewModel.screenLockSetting.value ?: 0).ordinal], false)

        binding.screenLockSetting.setOnItemClickListener { _, _, position, _ ->
            ScreenLockSetting.byPosition(position).let {
                viewModel.screenLockSetting.value = it.seconds
            }
        }

        binding.bitcoinElectrumServerPlaceholder = AppSettingsViewModel.DEFAULT_BITCOIN_ELECTRUM_URL
        binding.liquidElectrumServerPlaceholder = AppSettingsViewModel.DEFAULT_LIQUID_ELECTRUM_URL
        binding.testnetElectrumServerPlaceholder = AppSettingsViewModel.DEFAULT_TESTNET_ELECTRUM_URL
        binding.testnetLiquidElectrumServerPlaceholder = AppSettingsViewModel.DEFAULT_TESTNET_LIQUID_ELECTRUM_URL

        binding.bitcoinSpvElectrumServerPlaceholder = AppSettingsViewModel.DEFAULT_MULTI_SPV_BITCOIN_URL
        binding.liquidSpvElectrumServerPlaceholder = AppSettingsViewModel.DEFAULT_MULTI_SPV_LIQUID_URL
        binding.testnetSpvElectrumServerPlaceholder = AppSettingsViewModel.DEFAULT_MULTI_SPV_TESTNET_URL
        binding.testnetLiquidSpvElectrumServerPlaceholder = AppSettingsViewModel.DEFAULT_MULTI_SPV_TESTNET_LIQUID_URL

        binding.buttonAnalyticsMoreInfo.setOnClickListener {
            ConsentBottomSheetDialogFragment.show(childFragmentManager, hideButtons = true)
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
            viewModel.saveSettings()
            popBackStack()
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
                        checkInvitationCode(dialogBinding.text.toString())
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

    private fun checkInvitationCode(userCode: String){
        try {
            countly.getRemoteConfigValueAsJsonArray("feature_lightning_codes")
                ?.mapNotNull { jsonElement ->
                    jsonElement.jsonPrimitive.contentOrNull?.takeIf { it.isNotBlank() }
                }?.any { code ->
                    code == userCode
                }?.also {
                    if (it) {
                        settingsManager.lightningCodeOverride = true
                        binding.invalidateAll()
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun navigateUp(){
        onBackCallback.handleOnBackPressed()
    }
}
