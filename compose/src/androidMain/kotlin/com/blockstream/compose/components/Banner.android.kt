package com.blockstream.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.common.data.Banner
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.AnimatedNullableVisibility

@Composable
@Preview
fun BannerPreview() {
    GreenTheme {
        GreenColumn {
            Banner(Banner.preview1)
            Banner(Banner.preview2)
            Banner(Banner.preview3)
        }
    }
}