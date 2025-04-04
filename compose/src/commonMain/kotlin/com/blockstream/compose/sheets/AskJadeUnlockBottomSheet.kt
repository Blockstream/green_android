package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.blockstream_jade_plus_device
import blockstream_green.common.generated.resources.id_jade_already_unlocked
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_qr_airgapped_mode
import blockstream_green.common.generated.resources.id_qr_pin_unlock
import blockstream_green.common.generated.resources.id_unlock_jade_to_continue
import blockstream_green.common.generated.resources.id_unlock_your_jade_to_continue
import com.blockstream.common.Urls
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.theme.textMedium
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.setResult
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskJadeUnlockBottomSheet(
    viewModel: GreenViewModel,
    isOnboarding: Boolean,
    onDismissRequest: () -> Unit,
) {

    val title = when {
        isOnboarding -> stringResource(Res.string.id_qr_airgapped_mode)
        else -> {
            stringResource(Res.string.id_unlock_jade_to_continue)
        }
    }

    GreenBottomSheet(
        title = title,
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Image(
                painter = painterResource(Res.drawable.blockstream_jade_plus_device),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )

            GreenColumn(
                padding = 0,
                space = 16,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                Text(
                    text = stringResource(Res.string.id_unlock_your_jade_to_continue),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = textMedium
                )

                GreenButton(
                    text = stringResource(Res.string.id_learn_more),
                    type = GreenButtonType.TEXT,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.postEvent(Events.OpenBrowser(Urls.HELP_JADE_AIRGAPPED))
                }

                GreenButton(
                    text = stringResource(Res.string.id_jade_already_unlocked),
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.WHITE,
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigateDestinations.AskJadeUnlock.setResult(true)
                    onDismissRequest()
                }

                GreenButton(
                    text = stringResource(Res.string.id_qr_pin_unlock),
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigateDestinations.AskJadeUnlock.setResult(false)
                    onDismissRequest()
                }
            }
        }
    }
}
