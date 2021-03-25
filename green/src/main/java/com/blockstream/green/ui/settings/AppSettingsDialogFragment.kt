package com.blockstream.green.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.blockstream.green.R
import com.blockstream.green.databinding.DialogAppSettingsBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class AppSettingsDialogFragment : BottomSheetDialogFragment() {

    companion object{
        const val DEFAULT_CUSTOM_ELECTRUM_URL = "blockstream.info:700"
    }

    @Inject
    lateinit var vmAssistedFactory: AppSettingsViewModel.AssistedFactory

    private val viewModel: AppSettingsViewModel by viewModels {
        AppSettingsViewModel.provideFactory(
            vmAssistedFactory, DEFAULT_CUSTOM_ELECTRUM_URL
        )
    }

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

        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.buttonSave.setOnClickListener {
            viewModel.saveSettings()
            dismiss()

            // TODO check if active connections exists and apply immediately
//            MaterialAlertDialogBuilder(
//                requireContext(),
//                R.style.ThemeOverlay_Green_MaterialImportantAlertDialog
//            )
//                .setTitle(R.string.id_important)
//                .setMessage(R.string.id_youll_need_to_back_up_your)
//                .setNegativeButton(R.string.id_back_up_recovery_phrase) { _, _ ->
//
//                }
//                .setPositiveButton(R.string.id_continue) { _, _ ->
//
//                }
//                .setOnDismissListener {
//                    dismiss()
//                }
//                .show()

        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        isCancelable = false
    }

}
