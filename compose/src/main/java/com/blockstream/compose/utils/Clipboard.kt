package com.blockstream.compose.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build

fun copyToClipboard(context: Context, label: String = "Green", content: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
        ClipData.newPlainText(label, content)
    )
}

fun getClipboard(context: Context, clearClipboard: Boolean = false): String? =
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
        it.primaryClip?.getItemAt(0)?.text?.toString()
    }.also {
        if (clearClipboard) {
            clearClipboard(context = context)
        }
    }

fun clearClipboard(context: Context) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            it.clearPrimaryClip()
        }
    }
}