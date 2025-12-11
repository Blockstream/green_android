package com.blockstream.compose.screens.recovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.house
import blockstream_green.common.generated.resources.id_make_sure_you_are_alone_and_no
import blockstream_green.common.generated.resources.id_safe_environment
import com.blockstream.common.models.recovery.RecoveryIntroViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.compose.components.GreenColumn
import org.jetbrains.compose.resources.stringResource

@Composable
@Preview
private fun ItemPreview() {
    GreenChromePreview {
        GreenColumn {
            Item(
                stringResource(Res.string.id_safe_environment),
                stringResource(Res.string.id_make_sure_you_are_alone_and_no),
                Res.drawable.house
            )
        }
    }
}

@Composable
@Preview
fun RecoveryIntroScreenPreview() {
    GreenAndroidPreview {
        RecoveryIntroScreen(viewModel = RecoveryIntroViewModelPreview.preview())
    }
}