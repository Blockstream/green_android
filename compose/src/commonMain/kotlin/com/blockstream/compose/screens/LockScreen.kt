package com.blockstream.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.brand
import blockstream_green.common.generated.resources.id_green_is_now_the_blockstream_app
import blockstream_green.common.generated.resources.id_unlock
import com.blockstream.compose.LocalBiometricState
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LockScreen(
    unlock: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val biometricsState = LocalBiometricState.current

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    val launchBiometrics = suspend {
        biometricsState?.launchUserPresencePrompt(getString(Res.string.id_green_is_now_the_blockstream_app)) {
            unlock()
        }
    }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            launchBiometrics()
        }
    }


    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .clickable {
                // Catch all clicks
            }) {

        Image(
            painter = painterResource(Res.drawable.brand),
            contentDescription = "Blockstream",
            modifier = Modifier
                .height(70.dp)
                .align(
                    Alignment.Center
                )
        )

        GreenButton(
            text = stringResource(Res.string.id_unlock),
            type = GreenButtonType.TEXT,
            modifier = Modifier
                .align(
                    Alignment.BottomCenter
                )
                .padding(bottom = 24.dp)
        ) {
            coroutineScope.launch {
                launchBiometrics()
            }
        }
    }
}
