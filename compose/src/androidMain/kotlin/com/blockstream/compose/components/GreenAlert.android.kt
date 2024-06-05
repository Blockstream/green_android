package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview


@Composable
@Preview
fun GreenAlertPreview() {
    GreenThemePreview {
        GreenColumn() {

            GreenAlert(
                title = "Important!",
            )

            GreenAlert(
                message = "This is a message",

                )


            GreenAlert(
                title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",

                )

            GreenAlert(
                title = "Lorem ipsum dolor sit amet",
                message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                onCloseClick = {

                }
            )

            GreenAlert(
                message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                onCloseClick = {

                }
            )

            GreenAlert(
                title = "Important!",
                message = "This is a message",
                icon = painterResource(id = R.drawable.warning),
                primaryButton = "Learn More",
                onCloseClick = {

                }
            )

            GreenAlert(
                title = stringResource(R.string.id_lightning_account),
                message = stringResource(R.string.id_the_lightning_service_is_currently_unavailable_we),
                icon = painterResource(id = R.drawable.lightning)
            )
        }
    }
}