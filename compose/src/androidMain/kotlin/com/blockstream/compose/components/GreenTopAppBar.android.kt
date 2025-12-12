package com.blockstream.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.note_pencil
import blockstream_green.common.generated.resources.signature
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.NavData

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun GreenTopAppScreenPreview() {
    GreenPreview {
        var hasBackStack by remember {
            mutableStateOf(true)
        }
        var appBar by remember {
            mutableStateOf(
                NavData(
                    title = "Title",
                    subtitle = "This is the Subtitle",
                    walletName = "Liquid Wallet Singlesig",
                    actions = listOf(
                        NavAction(title = "Action", isMenuEntry = false),
                        NavAction(
                            title = "Action",
                            isMenuEntry = false,
                            icon = Res.drawable.note_pencil
                        ),
                        NavAction(
                            title = "Action Menu",
                            isMenuEntry = true,
                            icon = Res.drawable.signature
                        ),
                        NavAction(
                            title = "Action Menu",
                            isMenuEntry = true,
                            icon = Res.drawable.note_pencil
                        ),
                    )
                )
            )
        }

        Scaffold(topBar = {
            GreenTopAppBar(navData = appBar, goBack = {
                appBar = appBar.copy(walletName = "My New Wallet")
            }, hasBackStack = hasBackStack)
        }) {
            Box(modifier = Modifier.padding(it)) {
                GreenColumn {
                    GreenButton("Set wallet name") {
                        appBar = appBar.copy(walletName = "My Wallet")
                    }
                    GreenButton("Clear wallet name") {
                        appBar = appBar.copy(walletName = null)
                    }
                    GreenButton("Toggle hasBackStack") {
                        hasBackStack = !hasBackStack
                    }
                }
            }
        }
    }
}
