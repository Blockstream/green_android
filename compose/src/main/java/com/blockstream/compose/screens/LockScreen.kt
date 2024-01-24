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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.sideeffects.BiometricsState
import org.koin.compose.koinInject

@Composable
fun LockScreen(
    unlock: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val dialog = LocalDialog.current
    val androidKeystore: AndroidKeystore =
        if (LocalInspectionMode.current) AndroidKeystore(context) else koinInject()

    val biometricsState = remember {
        BiometricsState(
            context = context,
            coroutineScope = scope,
            snackbarHostState = snackbar,
            dialogState = dialog,
            androidKeystore = androidKeystore
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    val launchBiometrics = {
        biometricsState.launchUserPresencePrompt(context.getString(R.string.id_unlock_green)) {
            unlock()
        }
    }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            launchBiometrics()
        }
    }


    Box(modifier = Modifier
        .background(MaterialTheme.colorScheme.background)
        .fillMaxSize()
        .clickable {
            // Catch all clicks
        }) {

        Image(
            painter = painterResource(id = R.drawable.brand),
            contentDescription = "Blockstream",
            modifier = Modifier
                .height(70.dp)
                .align(
                    Alignment.Center
                )
        )

        GreenButton(
            text = stringResource(id = R.string.id_unlock),
            type = GreenButtonType.TEXT,
            modifier = Modifier
                .align(
                    Alignment.BottomCenter
                )
                .padding(bottom = 24.dp)
        ) {
            launchBiometrics()
        }
    }
}

@Composable
@Preview
fun LockScreenPreview() {
    GreenPreview {
        LockScreen()
    }
}