@file:OptIn(ExperimentalMaterial3Api::class)

package com.blockstream.compose.sideeffects

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.ExperimentalMaterial3Api
import com.blockstream.compose.R

private fun openBrowser(context: Context, url: String) {
    try {
        val builder = CustomTabsIntent.Builder()
        builder.setShowTitle(true)
        builder.setUrlBarHidingEnabled(false)
        builder.setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder()
//                    .setToolbarColor(ContextCompat.getColor(context, R.color.brand_surface))
//                    .setNavigationBarColor(ContextCompat.getColor(context, R.color.brand_surface))
//                    .setNavigationBarDividerColor(
//                        ContextCompat.getColor(
//                            context,
//                            R.color.brand_green
//                        )
//                    )
                .build()
        )
//            builder.setStartAnimations(context, R.anim.enter_slide_up, R.anim.fade_out)
//            builder.setExitAnimations(context, R.anim.fade_in, R.anim.exit_slide_down)

        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

suspend fun openBrowser(context: Context, dialogState: DialogState, isTor: Boolean, url: String) {
    if (isTor) {

        dialogState.openDialog(OpenDialogData(
            title = context.getString(R.string.id_tor),
            message = context.getString(R.string.id_you_have_tor_enabled_are_you),
            primaryText = context.getString(R.string.id_open),
            onPrimary = {
                openBrowser(context = context, url = url)
            },
            secondaryText = context.getString(R.string.id_cancel)
        ))

    } else {
        openBrowser(context = context, url = url)
    }
}