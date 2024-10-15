package com.blockstream.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.note_pencil
import blockstream_green.common.generated.resources.signature
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.LocalAppBarState
import com.blockstream.compose.utils.AppBarState

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun GreenTopAppScreenPreview() {
    GreenPreview {
        val appBarState = remember {
            AppBarState(
                NavData(
                    title = "Title",
                    subtitle = "This is the Subtitle",
                    actions = listOf(
                        NavAction(title = "Action", isMenuEntry = false),
                        NavAction(title = "Action", isMenuEntry = false, icon = Res.drawable.note_pencil),
                        NavAction(title = "Action Menu", isMenuEntry = true, icon =  Res.drawable.signature),
                        NavAction(title = "Action Menu", isMenuEntry = true, icon =  Res.drawable.note_pencil),
                    )
                )
            )
        }

        CompositionLocalProvider(LocalAppBarState provides appBarState) {
            Scaffold(topBar = {
                GreenTopAppBar()
            }) {
                Box(modifier = Modifier.padding(it)) {

                }
            }
        }
    }
}
