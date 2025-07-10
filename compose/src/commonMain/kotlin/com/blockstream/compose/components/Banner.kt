package com.blockstream.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_learn_more
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Warning
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.green.data.banner.Banner
import org.jetbrains.compose.resources.stringResource

@Composable
fun Banner(viewModel: GreenViewModel, withTopPadding: Boolean = false) {
    val bannerOrNull by viewModel.banner.collectAsStateWithLifecycle()

    AnimatedNullableVisibility(bannerOrNull) { banner ->
        Banner(
            banner,
            modifier = Modifier.padding(top = if (withTopPadding) 16.dp else 0.dp),
            onClick = {
                viewModel.postEvent(Events.BannerAction)
            },
            onClose = {
                viewModel.postEvent(Events.BannerDismiss)
            })
    }
}

@Composable
fun Banner(
    banner: Banner,
    modifier: Modifier = Modifier,
    onClick: (url: String) -> Unit = {},
    onClose: () -> Unit = {},
) {
    GreenAlert(
        modifier = modifier,
        title = banner.title,
        message = banner.message,
        maxLines = 5,
        icon = if (banner.isWarning) PhosphorIcons.Regular.Warning else null,
        primaryButton = if (banner.link != null) stringResource(Res.string.id_learn_more) else null,
        onPrimaryClick = {
            onClick.invoke(banner.link ?: "")
        },

        onCloseClick = if (banner.dismissable == true) {
            {
                onClose.invoke()
            }
        } else null
    )
}
