package com.blockstream.compose.screens.lightning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_authenticate
import blockstream_green.common.generated.resources.id_no_personal_data_will_be_shared
import blockstream_green.common.generated.resources.id_you_can_use_your_wallet_to
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LnUrlAuthRequestDataSerializable
import com.blockstream.common.events.Events
import com.blockstream.common.models.lightning.LnUrlAuthViewModel
import com.blockstream.common.models.lightning.LnUrlAuthViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


data class LnUrlAuthScreen(
    val greenWallet: GreenWallet,
    val requestData: LnUrlAuthRequestDataSerializable
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<LnUrlAuthViewModel>() {
            parametersOf(greenWallet, requestData.deserialize())
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        LnUrlAuthScreen(viewModel = viewModel)
    }
}

@Composable
fun LnUrlAuthScreen(
    viewModel: LnUrlAuthViewModelAbstract,
) {

    HandleSideEffect(viewModel)

    ConstraintLayout(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        val (topContent, middleContent, bottomContent) = createRefs()

        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        AnimatedVisibility(visible = onProgress, modifier = Modifier
            .fillMaxWidth()
            .constrainAs(topContent) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
            }) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
            )
        }


        GreenColumn(modifier = Modifier.constrainAs(middleContent) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            top.linkTo(parent.top)
            bottom.linkTo(bottomContent.top)
        }) {
            Text(
                stringResource(Res.string.id_you_can_use_your_wallet_to),
                textAlign = TextAlign.Center,
                style = bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = viewModel.requestData.domain,
                style = titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

        }

        GreenColumn(modifier = Modifier.constrainAs(bottomContent) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
        }) {
            GreenColumn(padding = 0) {
                Text(
                    stringResource(Res.string.id_no_personal_data_will_be_shared),
                    textAlign = TextAlign.Center,
                    style = bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
                GreenButton(
                    text = stringResource(Res.string.id_authenticate),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    viewModel.postEvent(Events.Continue)
                }
            }
        }

    }
}