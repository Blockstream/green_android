package com.blockstream.green.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.toSpanned
import androidx.core.text.underline
import androidx.fragment.app.Fragment
import com.blockstream.common.Urls
import com.blockstream.common.data.ApplicationSettings
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.BuildConfig
import com.blockstream.green.R
import com.blockstream.green.ui.AppFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.mohamedrejeb.ksoup.entities.KsoupEntities

fun Fragment.openNewTicket(
    settingsManager: SettingsManager,
    subject: String? = null,
    errorReport: ErrorReport? = null,
    isJade: Boolean = false,
) {
    val product = if (isJade) "blockstream_jade" else "green"
    val hw = if (isJade) "jade" else ""

    val policy: String = errorReport?.zendeskSecurityPolicy ?: ""

    openBrowser(
        settingsManager.getApplicationSettings(),
        String.format(
            Urls.BLOCKSTREAM_HELP_NEW_REQUEST,
            "android",
            subject?.let { KsoupEntities.encodeHtml(it) } ?: "",
            product,
            hw,
            BuildConfig.VERSION_NAME,
            policy
        )
    )
}

fun AppFragment<*>.openBrowser(url: String) {
    openBrowser(settingsManager.getApplicationSettings(), url)
}

fun Fragment.openBrowser(appSettings: ApplicationSettings, url: String) {

    val openBrowserBlock = { context: Context ->
        try {
            val builder = CustomTabsIntent.Builder()
            builder.setShowTitle(true)
            builder.setUrlBarHidingEnabled(false)
            builder.setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(context, R.color.brand_surface))
                    .setNavigationBarColor(ContextCompat.getColor(context, R.color.brand_surface))
                    .setNavigationBarDividerColor(
                        ContextCompat.getColor(
                            context,
                            R.color.brand_green
                        )
                    )
                    .build()
            )
            builder.setStartAnimations(context, R.anim.enter_slide_up, R.anim.fade_out)
            builder.setExitAnimations(context, R.anim.fade_in, R.anim.exit_slide_down)

            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (appSettings.tor) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.id_tor)
            .setMessage(R.string.id_you_have_tor_enabled_are_you)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                openBrowserBlock.invoke(requireContext())
            }
            .setNeutralButton(R.string.id_copy_url) { _, _ ->
                copyToClipboard("URL", url, requireContext())
            }
            .setNegativeButton(R.string.id_cancel) { _, _ ->

            }
            .show()
    } else {
        openBrowserBlock.invoke(requireContext())
    }
}


fun getClipboard(context: Context): String? =
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let {
        it.primaryClip?.getItemAt(0)?.text?.toString()
    }

fun copyToClipboard(label: String, content: String, context: Context, animateView: View? = null) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.also {
        it.setPrimaryClip(ClipData.newPlainText(label, content))
        animateView?.pulse()
    }
}

fun Int.dp(view: View) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), view.resources.displayMetrics).toInt()

fun Context.toPixels(size: Int) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), resources.displayMetrics)
        .toInt()

fun Fragment.toPixels(size: Int) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), resources.displayMetrics)
        .toInt()


val isDebug by lazy { BuildConfig.DEBUG }
val isDevelopmentFlavor by lazy { BuildConfig.FLAVOR == "development" || BuildConfig.APPLICATION_ID.contains(".dev") }
val isDevelopmentOrDebug by lazy { isDevelopmentFlavor || isDebug }
val isProductionFlavor by lazy { !isDevelopmentFlavor }
