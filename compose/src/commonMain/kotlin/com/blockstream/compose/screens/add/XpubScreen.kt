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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.add.XpubViewModel
import com.blockstream.common.models.add.XpubViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class XpubScreen(
    val greenWallet: GreenWallet,
    val setupArgs: SetupArgs
) : Parcelable, Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<XpubViewModel> {
            parametersOf(greenWallet, setupArgs)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        XpubScreen(viewModel = viewModel)
    }
}

@Composable
fun XpubScreen(
    viewModel: XpubViewModelAbstract
) {

    CameraBottomSheet.getResult {
        viewModel.xpub.value = it.result
    }

    HandleSideEffect(viewModel)

    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current

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
                    bottomSheetNavigator?.show(
                        CameraBottomSheet(
                            isDecodeContinuous = false,
                            parentScreenName = viewModel.screenName(),
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
