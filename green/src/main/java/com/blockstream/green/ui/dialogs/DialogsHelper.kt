package com.blockstream.green.ui.dialogs

import android.content.DialogInterface
import androidx.fragment.app.Fragment
import com.blockstream.green.R
import com.blockstream.green.settings.SettingsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/*
 * Singlesig / Electrum wallets don't support Tor connections, warn users if they have enabled
 * Tor connection in AppSettings
 */
fun Fragment.showTorSinglesigWarningIfNeeded(settingsManager: SettingsManager){

    if(settingsManager.showTorSinglesigWarning()) {
        val checkboxTitle = arrayOf(getString(R.string.id_dont_ask_me_again))
        var dontAskAgain = false
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.id_tor_is_not_available)
            .setMultiChoiceItems(
                checkboxTitle,
                booleanArrayOf(false)
            ) { _: DialogInterface, _: Int, checked: Boolean ->
                dontAskAgain = checked
            }
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, i: Int ->
                if (dontAskAgain) {
                    settingsManager.setTorSinglesigWarned()
                }
            }
            .show()
    }
}