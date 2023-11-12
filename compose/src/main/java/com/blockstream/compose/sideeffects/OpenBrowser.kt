@file:OptIn(ExperimentalMaterial3Api::class)

package com.blockstream.compose.sideeffects

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.compose.R
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

data class OpenBrowserData(
    val url: String,
    val continuation: CancellableContinuation<Unit>
){
    fun dismiss(){
        if(continuation.isActive){
            continuation.resume(Unit)
        }
    }
}

@Stable
class OpenBrowserState {

    private val mutex = Mutex()
    var data by mutableStateOf<OpenBrowserData?>(null)
        private set

    fun clear() {
        data?.dismiss()
        data = null
    }

    fun openBrowser(context: Context, url: String) {
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
        } finally {
            clear()
        }
    }

    suspend fun openBrowser(context: Context, isTor: Boolean, url: String) = mutex.withLock {
        if (isTor) {
            try {
                return suspendCancellableCoroutine { continuation ->
                    data = OpenBrowserData(url, continuation)
                }
            } finally {
                data = null
            }
        } else {
            openBrowser(context, url)
        }
    }
}

@Composable
fun OpenBrowserHost(state: OpenBrowserState) {
    val context = LocalContext.current
    state.data?.also {
        AlertDialog(
            title = {
                Text(text = stringResource(R.string.id_tor))
            },
            text = {
                Text(text = stringResource(R.string.id_you_have_tor_enabled_are_you))
            },
            onDismissRequest = {
                state.clear()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.openBrowser(context = context, url = it.url)
                    }
                ) {
                    Text(stringResource(R.string.id_open))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        state.clear()
                    }
                ) {
                    Text(stringResource(R.string.id_cancel))
                }
            }
        )
    }
}