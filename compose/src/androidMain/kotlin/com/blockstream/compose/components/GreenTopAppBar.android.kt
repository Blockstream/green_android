package com.blockstream.compose.components

import com.blockstream.compose.navigation.MenuIcon
import com.blockstream.compose.utils.StringHolder
import com.blockstream.compose.utils.toStringHolder
import com.blockstream.compose.events.Events
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import blockstream_green.common.generated.resources.Res
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
                    title = "Title".toStringHolder(),
                    subtitle = "This is the Subtitle".toStringHolder(),
                    walletName = "Liquid Wallet Singlesig",
                    actions = listOf(
                        NavAction.Help(event = { Events.Continue }),
                        NavAction.Note(hasNote = false, event = { Events.Continue }),
                        NavAction.Menu(
                            title = StringHolder(string = "Action Menu"),
                            icon = MenuIcon.Sign,
                            event = { Events.Continue }
                        ),
                        NavAction.Menu(
                            title = StringHolder(string = "Action Menu"),
                            icon = MenuIcon.Rename,
                            event = { Events.Continue }
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
