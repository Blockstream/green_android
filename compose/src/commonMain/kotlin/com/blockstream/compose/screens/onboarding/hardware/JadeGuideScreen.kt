package com.blockstream.compose.screens.onboarding.hardware

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_back_up_recovery_phrase
import blockstream_green.common.generated.resources.id_exit_guide
import blockstream_green.common.generated.resources.id_initialize_and_create_wallet
import blockstream_green.common.generated.resources.id_note_down_your_recovery_phrase
import blockstream_green.common.generated.resources.id_select_initialize_and_choose_to
import blockstream_green.common.generated.resources.id_step_1s
import blockstream_green.common.generated.resources.id_use_the_jogwheel_to_select_the
import blockstream_green.common.generated.resources.id_verify_recovery_phrase
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.devices.JadeGuideViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.components.GreenColumn
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun JadeGuideScreen(
    viewModel: JadeGuideViewModelAbstract
) {
    var step by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(step) {
        delay(5.toDuration(DurationUnit.SECONDS))
        step = (step + 1).takeIf { it < 3 } ?: 0
    }

    SetupScreen(viewModel = viewModel, withPadding = false) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.weight(1f), contentAlignment = Alignment.Center
            ) {
                if (!LocalInspectionMode.current) {
                    (0 until 3).forEach {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = step == it,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Rive(
                                riveAnimation = when (it) {
                                    0 -> RiveAnimation.JADE_PLUS_BUTTON
                                    1 -> RiveAnimation.RECOVERY_PHRASE
                                    else -> RiveAnimation.JADE_PLUS_SCROLL
                                }
                            )
                        }
                    }
                }
            }

            GreenColumn {

                GreenColumn(padding = 0, space = 8) {

                    (0 until 3).forEachIndexed { index, s ->
                        GreenCard(
                            onClick = {
                                step = s
                            }, border = BorderStroke(
                                1.dp,
                                if (step == s) green else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(Res.string.id_step_1s, (index + 1)),
                                    color = green,
                                    style = labelMedium,
                                    textAlign = TextAlign.Center
                                )

                                when (s) {
                                    0 -> Res.string.id_initialize_and_create_wallet
                                    1 -> Res.string.id_back_up_recovery_phrase
                                    else -> Res.string.id_verify_recovery_phrase
                                }.also {
                                    Text(
                                        stringResource(it),
                                        textAlign = TextAlign.Center,
                                        style = labelLarge,
                                        color = if (step == s) whiteHigh else whiteMedium
                                    )
                                }

                                AnimatedVisibility(visible = step == s) {
                                    when (s) {
                                        0 -> Res.string.id_select_initialize_and_choose_to
                                        1 -> Res.string.id_note_down_your_recovery_phrase
                                        else -> Res.string.id_use_the_jogwheel_to_select_the
                                    }.also {
                                        Text(
                                            stringResource(it),
                                            textAlign = TextAlign.Center,
                                            style = bodyMedium,
                                            color = whiteMedium,
                                            minLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                GreenButton(
                    text = stringResource(Res.string.id_exit_guide),
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    size = GreenButtonSize.BIG,
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.WHITE,
                ) {
                    viewModel.postEvent(Events.NavigateBack)
                }
            }
        }
    }
}