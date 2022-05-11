package com.blockstream.green.ui.bottomsheets

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.data.Countly
import com.blockstream.green.databinding.ConsentBottomSheetBinding
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.utils.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject

@AndroidEntryPoint
class ConsentBottomSheetDialogFragment: AbstractBottomSheetDialogFragment<ConsentBottomSheetBinding>(){

    override val screenName = "Consent"

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun inflate(layoutInflater: LayoutInflater): ConsentBottomSheetBinding = ConsentBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.detailsAreVisible = false
        binding.hideButtons = arguments?.getBoolean(HIDE_BUTTONS, false)
        binding.isDevelopment = isDevelopmentFlavor()

        // Make it swipe-dismissible
        isCancelable = settingsManager.isAskedAboutAnalyticsConsent()



        binding.switcher.setOnClickListener {
            (!(binding.detailsAreVisible as Boolean)).let { detailsAreVisible ->
                binding.detailsAreVisible = detailsAreVisible
                // Expand bottom sheet to make buttons visible
                if(detailsAreVisible){
                    (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        binding.buttonLearnMore.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.HELP_WHATS_COLLECTED)
        }

        binding.buttonDisable.setOnClickListener {
            dismiss()
        }

        binding.buttonEnable.setOnClickListener {
            settingsManager.saveApplicationSettings(
                settingsManager.getApplicationSettings().copy(analytics = true)
            )
            dismiss()
        }

        binding.buttonMenu.setOnClickListener {
            showPopupMenu(view, R.menu.menu_consent) { item ->
                when (item.itemId) {
                    R.id.copy_device_id -> {
                        countly.deviceId.let { deviceId ->
                            copyToClipboard("DeviceID", deviceId, binding.buttonMenu)
                            toast("DeviceID copied to Clipboard $deviceId")
                        }
                    }
                    R.id.zero_offset -> {
                        settingsManager.zeroCountlyOffset()
                        countly.updateOffset()
                        toast("Countly offset reset to zero")
                    }
                    R.id.reset_device_id -> {
                        countly.resetDeviceId()
                        toast("DeviceID reset. New DeviceId ${countly.deviceId}")
                    }
                }
                true
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        settingsManager.setAskedAboutAnalyticsConsent()

        (parentFragment ?: activity)?.let {
            if(it is DismissBottomSheetDialogListener){
                it.dialogDismissed(this)
            }
        }
    }

    companion object : KLogging() {
        private const val HIDE_BUTTONS = "HIDE_BUTTONS"

        fun show(fragmentManager: FragmentManager, hideButtons: Boolean = false) {
            show(ConsentBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putBoolean(HIDE_BUTTONS, hideButtons)
                }
            }, fragmentManager)
        }

        fun shouldShowConsentDialog(countly: Countly, settingsManager: SettingsManager): Boolean {
            return countly.analyticsFeatureEnabled && (!settingsManager.isAskedAboutAnalyticsConsent() && !settingsManager.getApplicationSettings().analytics)
        }
    }
}