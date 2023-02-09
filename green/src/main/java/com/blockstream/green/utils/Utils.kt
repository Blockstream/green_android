package com.blockstream.green.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.text.Spannable
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.inSpans
import androidx.core.text.toSpanned
import androidx.fragment.app.Fragment
import com.blockstream.green.BuildConfig
import com.blockstream.green.R
import com.blockstream.green.settings.ApplicationSettings
import com.blockstream.green.ui.AppFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import java.security.SecureRandom
import kotlin.random.Random

val SecureRandom by lazy { Random(SecureRandom().nextInt()) }

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
            .setPositiveButton(R.string.id_continue) { _, _ ->
                openBrowserBlock.invoke(requireContext())
            }
            .setNeutralButton(R.string.id_copy_to_clipboard) { _, _ ->
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
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
        it.primaryClip?.getItemAt(0)?.text?.toString()
    }

fun copyToClipboard(label: String, content: String, context: Context, animateView: View? = null) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
        it.setPrimaryClip(ClipData.newPlainText(label, content))
        animateView?.pulse()
    }
}

fun clearClipboard(context: Context) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
        it.clearPrimaryClip()
    }
}

fun notImpementedYet(context: Context) {
    if (isDevelopmentFlavor) {
        Toast.makeText(context, "Feature not Implemented", Toast.LENGTH_SHORT).show()
    }
}

fun createQrBitmap(content: String, errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M): Bitmap? {
    try {
        val matrix = Encoder.encode(content, errorCorrectionLevel).matrix

        val height: Int = matrix.height
        val width: Int = matrix.width
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (matrix[x, y].toInt() == 1) Color.BLACK else Color.WHITE
                )
            }
        }

        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun Int.dp(context: Context) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics).toInt()

fun Int.dp(view: View) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), view.resources.displayMetrics).toInt()

fun Context.toPixels(size: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, resources.displayMetrics).toInt()

fun Context.toPixels(size: Int) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), resources.displayMetrics)
        .toInt()

fun Fragment.toPixels(size: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, resources.displayMetrics).toInt()

fun Fragment.toPixels(size: Int) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), resources.displayMetrics)
        .toInt()

fun String?.nameCleanup(): String? = if (isNullOrBlank()) null else trim().replace("\n", "")

val isDebug by lazy { BuildConfig.DEBUG }
val isDevelopmentFlavor by lazy { BuildConfig.FLAVOR == "development" || BuildConfig.APPLICATION_ID.contains(".dev") }
val isDevelopmentOrDebug by lazy { isDevelopmentFlavor || isDebug }
val isProductionFlavor by lazy { !isDevelopmentFlavor }

fun Fragment.notifyDevelopmentFeature(message: String) {
    requireContext().notifyDevelopmentFeature(message)
}

fun Context.notifyDevelopmentFeature(message: String) {
    if (isDevelopmentFlavor) {
        Toast.makeText(this, "Development Flavor: $message", Toast.LENGTH_SHORT).show()
    }
}

fun Fragment.greenText(whiteText: Int, vararg greenTexts: Int): Spanned {
    return requireContext().greenText(whiteText, *greenTexts)
}

fun Context.greenText(whiteText: Int, vararg greenTexts: Int): Spanned {
    val whiteString = getString(whiteText)

    return try{
        buildSpannedString {
            color(ContextCompat.getColor(this@greenText, R.color.white)) {
                append(whiteString)
            }

            greenTexts.map { getString(it).lowercase() }.forEach {
                val start = whiteString.lowercase().indexOf(it)
                setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            this@greenText,
                            R.color.brand_green
                        )
                    ),
                    start,
                    start + it.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }.toSpanned()
    }catch (e: Exception){
        whiteString.toSpanned()
    }
}

fun Context.linkedText(whiteText: Int, links: List<Pair<Int, ClickableSpan>>): Spanned {
    val whiteString = getString(whiteText)

    return try {
        buildSpannedString {
            color(ContextCompat.getColor(this@linkedText, R.color.white)) {
                append(whiteString)
            }

            links.onEach { link ->
                val text = getString(link.first).lowercase()
                val start = whiteString.lowercase().indexOf(text)

                listOf(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            this@linkedText,
                            R.color.brand_green
                        )
                    ), link.second
                ).onEach {
                    setSpan(
                        it,
                        start,
                        start + text.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }.toSpanned()
    } catch (e: Exception) {
        whiteString.toSpanned()
    }
}