package com.blockstream.green.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast

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

fun String?.nameCleanup(): String? = if (isNullOrBlank()) null else trim().replace("\n", "")

fun Context.isDevelopmentFlavor() = packageName.contains(".dev")
