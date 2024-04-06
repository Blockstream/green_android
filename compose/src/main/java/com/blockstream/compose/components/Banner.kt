package com.blockstream.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.common.data.Banner
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.AnimatedNullableVisibility

@Composable
fun Banner(viewModel: GreenViewModel) {
    val bannerOrNull by viewModel.banner.collectAsStateWithLifecycle()

    AnimatedNullableVisibility(bannerOrNull) { banner ->
        Banner(banner, modifier = Modifier.padding(top = 16.dp), onClick = {
            viewModel.postEvent(Events.BannerAction)
        }, onClose = {
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
    Card(modifier = Modifier.fillMaxWidth().then(modifier)) {
        Box {
            GreenRow(
                padding = 0,
                modifier = Modifier.padding(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (banner.link.isNullOrBlank()) 16.dp else 8.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                if (banner.isWarning) {
                    Icon(
                        painter = painterResource(id = R.drawable.warning),
                        contentDescription = null,
                    )
                }

                GreenColumn(space = 0, padding = 0) {
                    banner.title?.also { title ->
                        Text(
                            text = title,
                            maxLines = 2,
                            style = titleSmall,
                            modifier = Modifier.padding(
                                end = if (banner.dismissable == true) 24.dp else 0.dp
                            )
                        )
                    }
                    banner.message?.also { message ->
                        Text(
                            text = message,
                            style = bodyLarge,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    banner.link?.also {
                        GreenButton(
                            text = stringResource(id = R.string.id_learn_more),
                            type = GreenButtonType.TEXT,
                            size = GreenButtonSize.SMALL,
                            modifier = Modifier
                                .padding(0.dp)
                                .align(Alignment.End)
                        ) {
                            onClick.invoke(it)
                        }
                    }
                }
            }

            if (banner.dismissable == true) {
                IconButton(
                    onClick = { onClose.invoke() }, modifier = Modifier
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.x),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}


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