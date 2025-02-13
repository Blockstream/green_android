package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_lightning_account
import blockstream_green.common.generated.resources.id_the_lightning_service_is_currently_unavailable_we
import blockstream_green.common.generated.resources.lightning
import blockstream_green.common.generated.resources.warning
import com.blockstream.compose.GreenPreview
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
@Preview
fun GreenAlertPreview() {
    GreenPreview {
        GreenColumn {

            GreenAlert(title = "Important!")

            GreenAlert(message = "This is a message")


            GreenAlert(
                title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",

                )

            GreenAlert(
                title = "Lorem ipsum dolor sit amet",
                message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                onCloseClick = {

                })

            GreenAlert(
                message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                onCloseClick = {

                })

            GreenAlert(
                title = "Important!",
                message = "This is a message",
                icon = painterResource(Res.drawable.warning),
                primaryButton = "Learn More",
                onCloseClick = {

                })

            GreenAlert(
                title = stringResource(Res.string.id_lightning_account),
                message = stringResource(Res.string.id_the_lightning_service_is_currently_unavailable_we),
                icon = painterResource(Res.drawable.lightning),
                isBlue = true
            )
        }
    }
}