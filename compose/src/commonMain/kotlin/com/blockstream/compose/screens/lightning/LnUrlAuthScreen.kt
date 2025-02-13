package com.blockstream.compose.screens.lightning

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.constraintlayout.compose.ConstraintLayout
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_authenticate
import blockstream_green.common.generated.resources.id_no_personal_data_will_be_shared
import blockstream_green.common.generated.resources.id_you_can_use_your_wallet_to
import com.blockstream.common.events.Events
import com.blockstream.common.models.lightning.LnUrlAuthViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.resources.stringResource

@Composable
fun LnUrlAuthScreen(
    viewModel: LnUrlAuthViewModelAbstract,
) {

    HandleSideEffect(viewModel)

    SetupScreen(viewModel = viewModel) {

        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val (middleContent, bottomContent) = createRefs()

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
}