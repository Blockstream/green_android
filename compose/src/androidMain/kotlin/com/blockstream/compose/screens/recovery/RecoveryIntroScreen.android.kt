package com.blockstream.compose.screens.recovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.house
import com.blockstream.common.models.recovery.RecoveryIntroViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenThemePreview


@Composable
@Preview
private fun ItemPreview() {
    GreenThemePreview {
        GreenColumn {
            Item(
                stringResource(id = R.string.id_safe_environment),
                stringResource(id = R.string.id_make_sure_you_are_alone_and_no),
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