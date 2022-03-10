package com.blockstream.green.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.blockstream.green.R
import com.blockstream.green.databinding.DialogAppSettingsBottomSheetBinding
import com.blockstream.green.utils.endIconCopyMode
import com.blockstream.green.utils.isDevelopmentFlavor
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AppSettingsDialogFragment : BottomSheetDialogFragment() {
    private val viewModel: AppSettingsViewModel by viewModels()

    private lateinit var binding: DialogAppSettingsBottomSheetBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.dialog_app_settings_bottom_sheet,
            container,
            false
        )

        binding.lifecycleOwner = viewLifecycleOwner

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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.isDevelopment = isDevelopmentFlavor()

        binding.personalBitcoinElectrumServerInputLayout.endIconCopyMode()
        binding.personalLiquidElectrumServerInputLayout.endIconCopyMode()
        binding.personalTestnetElectrumServerInputLayout.endIconCopyMode()
        binding.personalTestnetLiquidElectrumServerInputLayout.endIconCopyMode()
        binding.proxyURLInputLayout.endIconCopyMode()
        binding.spvBitcoinElectrumServerInputLayout.endIconCopyMode()
        binding.spvLiquidElectrumServerInputLayout.endIconCopyMode()
        binding.spvTestnetElectrumServerInputLayout.endIconCopyMode()
        binding.spvTestnetLiquidElectrumServerInputLayout.endIconCopyMode()

        binding.buttonSave.setOnClickListener {
            viewModel.saveSettings()
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        isCancelable = false
    }
}