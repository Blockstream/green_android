package com.blockstream.compose.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build

fun copyToClipboard(context: Context, label: String, content: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
        ClipData.newPlainText(label, content)
    )
}

fun getClipboard(context: Context): String? =
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
        it.primaryClip?.getItemAt(0)?.text?.toString()
    }

fun clearClipboard(context: Context) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            it.clearPrimaryClip()
        }
    }
}