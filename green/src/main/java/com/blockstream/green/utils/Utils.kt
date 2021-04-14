package com.blockstream.green.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.TypedValue
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.blockstream.green.R
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder


fun openBrowser(context: Context, url: String) {
    try {
        val builder = CustomTabsIntent.Builder()
        builder.setShowTitle(true)
        builder.setUrlBarHidingEnabled(false)
        builder.setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.brand_surface))
            .setNavigationBarColor(ContextCompat.getColor(context, R.color.brand_surface))
            .setNavigationBarDividerColor(ContextCompat.getColor(context, R.color.brand_green))
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


fun getClipboard(context: Context): String? =
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
        it.primaryClip?.getItemAt(0)?.text?.toString()
    }

fun copyToClipboard(label: String, content: String, context: Context) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
        it.setPrimaryClip(ClipData.newPlainText(label, content))
    }
}

fun getVersionName(context: Context): String {
    val manager = context.packageManager
    val info = manager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
    return info.versionName
}

fun notImpementedYet(context: Context) {
    if(context.isDevelopmentFlavor()) {
        Toast.makeText(context, "Feature not Implemented", Toast.LENGTH_SHORT).show()
    }
}

fun createQrBitmap(content: String): Bitmap? {
    try {
        val matrix = Encoder.encode(content, ErrorCorrectionLevel.M).matrix

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

fun Fragment.toPixels(size: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, resources.displayMetrics).toInt()

fun Fragment.toPixels(size: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), resources.displayMetrics).toInt()

fun String?.nameCleanup(): String? = if (isNullOrBlank()) null else trim().replace("\n", "")

fun Context.isDevelopmentFlavor() = packageName.contains(".dev")
fun Context.isProductionFlavor() = !this.isDevelopmentFlavor()

fun Fragment.isDevelopmentFlavor() = requireContext().isDevelopmentFlavor()
fun Fragment.isProductionFlavor() = !this.isDevelopmentFlavor()
