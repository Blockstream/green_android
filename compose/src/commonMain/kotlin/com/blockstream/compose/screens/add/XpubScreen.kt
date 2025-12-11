package com.blockstream.compose.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_enter_your_xpub
import blockstream_green.common.generated.resources.id_use_an_xpub_for_which_you_own
import blockstream_green.common.generated.resources.id_xpub
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.models.add.XpubViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.getResult
import org.jetbrains.compose.resources.stringResource

@Composable
fun XpubScreen(
    viewModel: XpubViewModelAbstract
) {

    NavigateDestinations.Camera.getResult<ScanResult> {
        viewModel.xpub.value = it.result
    }

    SetupScreen(viewModel = viewModel, withPadding = false) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                Text(stringResource(Res.string.id_enter_your_xpub), style = displayMedium)

                Text(stringResource(Res.string.id_use_an_xpub_for_which_you_own), style = bodyLarge)

                val xpub by viewModel.xpub.collectAsStateWithLifecycle()
                val error by viewModel.error.collectAsStateWithLifecycle()
                GreenTextField(
                    title = stringResource(Res.string.id_xpub),
                    value = xpub,
                    onValueChange = viewModel.xpub.onValueChange(),
                    singleLine = false,
                    error = error,
                    onQrClick = {
                        viewModel.postEvent(
                            NavigateDestinations.Camera(
                                isDecodeContinuous = false,
                                parentScreenName = viewModel.screenName()
                            )
                        )
                    }
                )
            }

            val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
            GreenButton(
                text = stringResource(Res.string.id_continue),
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }
    }
}
