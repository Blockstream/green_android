package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockstream.common.data.ScanResult
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.settings.AppSettingsViewModel
import com.blockstream.compose.screens.settings.AppSettingsScreen
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel


class AppSettingsFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, R.menu.app_settings) {
    private val viewModel: AppSettingsViewModel by viewModel()

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val sideEffectsHandledByAppFragment: Boolean = true

    override val useCompose: Boolean = true

    private val onBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            viewModel.postEvent(AppSettingsViewModel.LocalEvents.OnBack)
        }
    }

//    override fun handleSideEffect(sideEffect: SideEffect) {
//        super.handleSideEffect(sideEffect)
//
//        if(sideEffect is AppSettingsViewModel.LocalSideEffects.UnsavedAppSettings){
//            MaterialAlertDialogBuilder(requireContext())
//                .setTitle(R.string.id_app_settings)
//                .setMessage(R.string.id_your_settings_are_unsavednndo)
//                .setPositiveButton(R.string.id_continue) { _, _ ->
//                    onBackCallback.isEnabled = false
//                    popBackStack()
//                }
//                .setNegativeButton(android.R.string.cancel, null)
//                .show()
//        }
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<ScanResult>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(
            viewLifecycleOwner
        ) { result ->
            result?.also { scannedCode ->
                viewModel.postEvent(AppSettingsViewModel.LocalEvents.InvitationCode(scannedCode.result))
            }
        }

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                GreenTheme {
                    BottomSheetNavigatorM3 {
                        AppSettingsScreen(viewModel = viewModel)
                    }
                }
            }
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
