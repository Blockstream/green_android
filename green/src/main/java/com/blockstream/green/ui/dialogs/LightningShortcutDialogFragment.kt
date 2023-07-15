package com.blockstream.green.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.common.Urls
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.databinding.LightningShortcutDialogBinding
import com.blockstream.green.utils.openBrowser
import mu.KLogging
import org.koin.android.ext.android.inject

interface EnableLightningShortcut{
    fun enableLightningShortcut()
    fun lightningShortcutDialogDismissed()
}

class LightningShortcutDialogFragment : AbstractDialogFragment<LightningShortcutDialogBinding>() {

    override fun inflate(layoutInflater: LayoutInflater): LightningShortcutDialogBinding =
        LightningShortcutDialogBinding.inflate(layoutInflater)

    override val screenName: String? = null

    override val isFullWidth: Boolean = true

    private val settingsManager: SettingsManager by inject()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.isAddAccount = arguments?.getBoolean(IS_ADD_ACCOUNT, false) ?: false

        binding.buttonNeutral.setOnClickListener {
            openBrowser(settingsManager.appSettings, Urls.HELP_LIGHTNING_SHORTCUT)
        }

        binding.buttonPositive.setOnClickListener {
            (parentFragment as? EnableLightningShortcut)?.enableLightningShortcut()
            dismiss()
        }

        binding.buttonNegative.setOnClickListener {
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (parentFragment as? EnableLightningShortcut)?.lightningShortcutDialogDismissed()
    }

    companion object : KLogging() {
        private const val IS_ADD_ACCOUNT = "IS_ADD_ACCOUNT"
        fun show(isAddAccount: Boolean = false, fragmentManager: FragmentManager) {
            showSingle(LightningShortcutDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putBoolean(IS_ADD_ACCOUNT, isAddAccount)
                }
            }, fragmentManager)
        }
    }
}

