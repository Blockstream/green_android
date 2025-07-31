package com.blockstream.compose.screens.overview.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_watchonly_description
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.md_theme_primary
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.ui.components.RichSpan
import com.blockstream.ui.components.RichText
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchOnlyWalletDescription(onClickLearnMore: () -> Unit) {
    val description = stringResource(Res.string.id_watchonly_description)
    val learnMore = stringResource(Res.string.id_learn_more)

    RichText(
        text = description,
        spans = listOf(
            RichSpan(
                text = learnMore, style = SpanStyle(color = md_theme_primary), onClick = onClickLearnMore
            )
        ),
        defaultStyle = bodyMedium.copy(color = whiteMedium, lineHeight = 24.sp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}