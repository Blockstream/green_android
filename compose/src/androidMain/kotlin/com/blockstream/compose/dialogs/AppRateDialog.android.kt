package com.blockstream.compose.dialogs

import android.app.Activity
import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.PlayableInstance
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_congratulations
import blockstream_green.common.generated.resources.id_you_have_successfully_sent_a
import blockstream_green.common.generated.resources.x
import com.blockstream.base.GooglePlay
import com.blockstream.common.Urls
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.LocalActivity
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.utils.HandleSideEffectDialog
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject


@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun AppRateDialog(
    viewModel: GreenViewModel,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            onDismissRequest()
        }
    ) {

        HandleSideEffectDialog(viewModel, onDismiss = {
            onDismissRequest()
        })

        GreenCard(modifier = Modifier.fillMaxWidth(), padding = 0) {

            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(all = 8.dp),
                onClick = {
                    onDismissRequest()
                }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.x),
                    contentDescription = "Close",
                )
            }

            GreenColumn(padding = 24) {
                if (!LocalInspectionMode.current) {
                    Rive(RiveAnimation.CHECKMARK)
                }

                GreenColumn(
                    padding = 0,
                    space = 8,
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.id_congratulations),
                        style = titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = stringResource(Res.string.id_you_have_successfully_sent_a),
                        style = bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    if (!LocalInspectionMode.current) {

                        val googlePlay: GooglePlay = koinInject()
                        val activity = LocalActivity.current as Activity

                        val riveFile = "files/rive/rating_animation.riv"
                        var bytes: ByteArray? by remember {
                            mutableStateOf(null)
                        }

                        AndroidView(factory = {
                            LayoutInflater.from(it)
                                .inflate(R.layout.rive, null)
                                .apply {
                                    val animationView: RiveAnimationView =
                                        findViewById(R.id.rive)
                                    animationView.setRiveResource(
                                        R.raw.rive_empty,
                                        stateMachineName = "State Machine 1",
                                        autoplay = true
                                    )

                                    var lastStateName = ""
                                    var handled = false

                                    animationView.registerListener(object :
                                        RiveFileController.Listener {
                                        override fun notifyLoop(animation: PlayableInstance) {
                                        }

                                        override fun notifyPause(animation: PlayableInstance) {
                                            val rate = when (lastStateName) {
                                                "1_star" -> 1
                                                "2_stars" -> 2
                                                "3_stars" -> 3
                                                "4_stars" -> 4
                                                "5_stars" -> 5
                                                else -> 0
                                            }

                                            if (rate > 0 && !handled) {
                                                handled = true
                                                ContextCompat.getMainExecutor(it).execute {
                                                    activity.also {
                                                        googlePlay.showInAppReviewDialog(it) {
                                                            viewModel.postEvent(
                                                                Events.OpenBrowser(
                                                                    Urls.BLOCKSTREAM_GOOGLE_PLAY
                                                                )
                                                            )
                                                        }
                                                    }
                                                    viewModel.settingsManager.setAskedAboutAppReview()
                                                    onDismissRequest()
                                                }
                                            }
                                        }

                                        override fun notifyPlay(animation: PlayableInstance) {

                                        }

                                        override fun notifyStateChanged(
                                            stateMachineName: String,
                                            stateName: String
                                        ) {
                                            lastStateName = stateName
                                        }

                                        override fun notifyStop(animation: PlayableInstance) {

                                        }
                                    })

                                }
                        }, update = {
                            bytes?.also { bytes ->
                                val animationView: RiveAnimationView =
                                    it.findViewById(R.id.rive)
                                animationView.setRiveBytes(bytes = bytes, autoplay = true)
                            }
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(start = 16.dp, bottom = 16.dp)
                        )

                        LaunchedEffect(riveFile) {
                            bytes = Res.readBytes(riveFile)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun AppRateDialogPreview() {
    GreenPreview {
        AppRateDialog(
            viewModel = SimpleGreenViewModelPreview()
        ) {

        }
    }
}