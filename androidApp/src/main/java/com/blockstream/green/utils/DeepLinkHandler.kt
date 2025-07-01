package com.blockstream.green.utils

import android.net.Uri
import com.blockstream.common.events.Events
import com.blockstream.common.models.MainViewModel
import com.blockstream.common.sideeffects.SideEffects

object DeepLinkHandler : Loggable() {

    fun handleDeepLink(uri: Uri?, mainViewModel: MainViewModel): Boolean {
        uri ?: return false

        logger.i { "Handling deep link: $uri" }

        return when {
            uri.scheme == "blockstream" -> handleBlockstreamScheme(uri, mainViewModel)
            else -> false
        }
    }

    private fun handleBlockstreamScheme(uri: Uri, mainViewModel: MainViewModel): Boolean {
        return when (uri.host) {
            "redirect" -> handleRedirect(uri, mainViewModel)
            else -> {
                logger.w { "Unknown blockstream:// host: ${uri.host}" }
                false
            }
        }
    }

    private fun handleRedirect(uri: Uri, mainViewModel: MainViewModel): Boolean {
        return when (uri.path) {
            "/transactions" -> {
                mainViewModel.postEvent(Events.EventSideEffect(sideEffect = SideEffects.NavigateToTransactTab))
                true
            }

            else -> {
                logger.w { "Unknown redirect path: ${uri.path}" }
                false
            }
        }
    }
}