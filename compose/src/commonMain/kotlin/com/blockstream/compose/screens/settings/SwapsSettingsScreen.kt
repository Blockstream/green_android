package com.blockstream.compose.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_disable_on_this_device
import blockstream_green.common.generated.resources.id_enable_swaps
import blockstream_green.common.generated.resources.id_get_more_out_of_jade
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_swaps_are_enabled_on_this_phone
import blockstream_green.common.generated.resources.id_swaps_enabled
import blockstream_green.common.generated.resources.id_unlock_swaps_from_this_wallet
import blockstream_green.common.generated.resources.id_you_cannot_disable_while_a_swap
import blockstream_green.common.generated.resources.swap_ln_lbtc
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CheckSquare
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.settings.SwapsSettingsViewModelAbstract
import com.blockstream.compose.models.settings.SwapsSettingsViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.textMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.data.Urls
import com.blockstream.data.data.GreenWallet
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SwapsSettingsScreen(
    viewModel: SwapsSettingsViewModelAbstract
) {
    NavigateDestinations.Login.getResult<GreenWallet> {
        viewModel.enable()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val title = if (uiState.isEnabled) Res.string.id_swaps_enabled else Res.string.id_get_more_out_of_jade
    val message = if (uiState.isEnabled) Res.string.id_swaps_are_enabled_on_this_phone else Res.string.id_unlock_swaps_from_this_wallet
    val buttonText = if (uiState.isEnabled) Res.string.id_disable_on_this_device else Res.string.id_enable_swaps

    SetupScreen(viewModel = viewModel) {

        GreenColumn(padding = 0, space = 8) {

            GreenColumn(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth()
                    .weight(1f), horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (uiState.isEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Rive(RiveAnimation.CHECKMARK) {
                            Icon(
                                modifier = Modifier
                                    .size(128.dp),
                                imageVector = PhosphorIcons.Regular.CheckSquare,
                                contentDescription = "Check",
                                tint = Color.White
                            )
                        }
                    }
                }

                GreenColumn(padding = 0, space = 6, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(title), style = titleMedium)
                    Text(text = stringResource(message), color = textMedium, textAlign = TextAlign.Center)
                }

                if (!uiState.isEnabled) {
                    Image(
                        modifier =
                            Modifier.align(Alignment.CenterHorizontally)
                                .padding(top = 24.dp),
                        painter = painterResource(Res.drawable.swap_ln_lbtc),
                        contentDescription = null
                    )
                }
            }


            if (uiState.isEnabled) {
                if (!uiState.canBeDisabled) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.id_you_cannot_disable_while_a_swap),
                        textAlign = TextAlign.Center,
                        color = whiteLow
                    )
                }

                GreenButton(
                    text = stringResource(buttonText), modifier = Modifier.fillMaxWidth(),
                    size = GreenButtonSize.BIG,
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.GREENER,
                    enabled = uiState.canBeDisabled
                ) {
                    viewModel.disable()
                }
            } else {

                GreenColumn(space = 16, padding = 0) {

                    GreenButton(
                        text = stringResource(buttonText), modifier = Modifier.fillMaxWidth(),
                        size = GreenButtonSize.BIG,
                    ) {
                        viewModel.enable()
                    }
                }
            }

            GreenButton(
                text = stringResource(Res.string.id_learn_more),
                modifier = Modifier.fillMaxWidth(),
                size = GreenButtonSize.BIG,
                type = GreenButtonType.TEXT
            ) {
                viewModel.postEvent(Events.OpenBrowser(Urls.HELP_PERFORM_SWAP))
            }
        }
    }
}

@Composable
@Preview
fun FeatureSettingsScreenPreview() {
    GreenPreview {
        SwapsSettingsScreen(viewModel = SwapsSettingsViewModelPreview.preview(isEnabled = false))
    }
}