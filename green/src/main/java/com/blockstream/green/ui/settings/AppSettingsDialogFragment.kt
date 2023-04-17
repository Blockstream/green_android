package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.blockstream.green.databinding.DialogAppSettingsBottomSheetBinding
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.ui.bottomsheets.AbstractBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.ConsentBottomSheetDialogFragment
import com.blockstream.green.utils.isDevelopmentFlavor
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging


@AndroidEntryPoint
class AppSettingsDialogFragment : AbstractBottomSheetDialogFragment<DialogAppSettingsBottomSheetBinding>() {
    override val screenName = "AppSettings"

    override val expanded: Boolean = true

    private val viewModel: AppSettingsViewModel by viewModels()

    override fun inflate(layoutInflater: LayoutInflater) = DialogAppSettingsBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel
        binding.isDevelopment = isDevelopmentFlavor

        val screenLockSettings = ScreenLockSetting.getStringList(requireContext())
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
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        isCancelable = false
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(AppSettingsDialogFragment(), fragmentManager)
        }
    }
}