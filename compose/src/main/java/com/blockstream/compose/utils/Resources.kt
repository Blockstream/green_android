package com.blockstream.compose.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.blockstream.compose.R

@Composable
@ReadOnlyComposable
fun stringResourceIdOrNull(id: String?): String? {
    if (id?.startsWith("id_") == true) {
        return stringResourceId(LocalContext.current, id)
    }
    return id
}

@Composable
@ReadOnlyComposable
fun stringResourceId(id: String): String {
    return stringResourceIdOrNull(id = id) ?: id
}

fun stringResourceId(context: Context, id: String): String {
    return stringResourceIdOrNull(context = context, id = id) ?: id
}

fun stringResourceIdOrNull(context: Context, id: String?): String? {
    if (id?.startsWith("id_") == true) {
        val res = id.substring(0, id.indexOf("|").takeIf { it != -1 } ?: id.length)
        val formatArgs = id.split("|").drop(1).map {
            (if(it.startsWith("id_")) stringResourceIdOrNull(context, it) else null) ?: it
        }.toTypedArray()

        val resources = context.resources

        val intRes = resources.getIdentifier(res, "string", context.packageName)
        if (intRes > 0) {
            return try {
                context.getString(intRes, *formatArgs)
            }catch (e: Exception){
                e.printStackTrace()
                id
            }
        }
    } else if (id?.contains("Breez SDK error", ignoreCase = true) == true){
        val message = try {
            id.substring(id.indexOf("message: "))
        } catch (e: Exception) {
            id.replace("Breez SDK error:", "")
        }
        context.getString(R.string.id_an_unidentified_error_occurred, message)
    }

    return id
}

@Composable
@ReadOnlyComposable
fun drawableResourceIdOrNull(id: String?): Int? {
    return drawableResourceIdOrNull(LocalContext.current, id)
}

fun drawableResourceIdOrNull(context: Context, id: String?): Int? {
    val resources = context.resources
    return resources.getIdentifier(id, "drawable", context.packageName).takeIf { it > 0 }
}

fun ByteArray?.toBitmap(): Bitmap?{
    if(this != null) {
        try {
            return BitmapFactory.decodeByteArray(this, 0, this.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}