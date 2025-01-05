package com.blockstream.compose.components

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.x
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.blockstream.common.data.Promo
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import org.jetbrains.compose.resources.painterResource

@Composable
fun Promo(
    viewModel: GreenViewModel,
    withAnimation: Boolean = false,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settingsManager.appSettingsStateFlow.collectAsStateWithLifecycle()
    val promoOrNull by viewModel.promo.collectAsStateWithLifecycle()

    if (withAnimation) {
        AnimatedNullableVisibility(
            value = promoOrNull.takeIf { !settings.tor },
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) { promo ->
            Promo(
                promo = promo,
                modifier = modifier,
                viewModel = viewModel
            )
        }
    } else {
        promoOrNull?.also {
            Promo(
                promo = it,
                modifier = modifier,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun Promo(
    promo: Promo,
    modifier: Modifier = Modifier,
    viewModel: GreenViewModel
) {

    val imageSmall = remember {
        promo.imageSmallFile
    }

    val painter = rememberAsyncImagePainter(imageSmall?.filePath)
    val state by painter.state.collectAsStateWithLifecycle()

    if (imageSmall == null || state is AsyncImagePainter.State.Success) {

        LaunchedEffect(Unit) {
            viewModel.postEvent(Events.PromoImpression)
        }

        GreenCard(
            padding = 0,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.then(modifier)
        ) {

            IconButton(
                onClick = {
                    viewModel.postEvent(Events.PromoDismiss)
                }, modifier = Modifier.padding(top = 2.dp, end = 2.dp)
                    .align(Alignment.TopEnd).zIndex(100f)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.x),
                    contentDescription = null,
                )
            }

            when (promo.layoutSmall) {
                0 -> PromoLayout0(promo, viewModel, painter)
                1 -> PromoLayout1(promo, viewModel, painter)
                2 -> PromoLayout2(promo, viewModel, painter)
            }

        }
    }
}

@Composable
fun ColumnScope.PromoCta(promo: Promo, viewModel: GreenViewModel) {
    promo.ctaSmall?.also { ctaSmall ->
        GreenButton(
            text = ctaSmall,
            size = GreenButtonSize.BIG,
            modifier = Modifier.fillMaxWidth()
                .padding(16.dp)
        ) {
            if (promo.isSmall) {
                viewModel.postEvent(Events.PromoAction)
            } else {
                viewModel.postEvent(Events.PromoOpen)
            }
        }
    }
}

@Composable
fun BoxScope.PromoLayout0(promo: Promo, viewModel: GreenViewModel, painter: AsyncImagePainter) {
    GreenColumn(
        padding = 0,
        space = 8,
        modifier = Modifier.padding(
            bottom = 16.dp
        )
    ) {

        Row {

            GreenColumn(
                padding = 0, space = 8, modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = if (promo.imageSmall.isNotBlank()) 0.dp else 36.dp,
                        top = 16.dp
                    )
                    .weight(1f)
            ) {

                Text(
                    text = promo.titleSmall ?: "",
                    style = titleMedium,
                )

                promo.textSmall?.also {
                    Text(
                        it, style = bodyMedium
                    )
                }
            }

            if (promo.imageSmall.isNotBlank()) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .height(100.dp)
                        .align(Alignment.Top)
                        .padding(end = 16.dp)
                )
            }
        }

        PromoCta(promo, viewModel)
    }
}

@Composable
fun BoxScope.PromoLayout1(promo: Promo, viewModel: GreenViewModel, painter: AsyncImagePainter) {
    GreenColumn(
        padding = 0,
        space = 8,
    ) {
        GreenColumn(
            padding = 0,
            space = 8,
            modifier = Modifier.padding(16.dp)
        ) {

            if (promo.imageSmall.isNotBlank()) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )
            }

            promo.titleSmall?.also {
                Text(
                    text = it,
                    style = titleMedium,
                )
            }

            promo.textSmall?.also {
                Text(
                    it, style = bodyMedium
                )
            }
        }

        PromoCta(promo, viewModel)
    }
}

@Composable
fun BoxScope.PromoLayout2(promo: Promo, viewModel: GreenViewModel, painter: AsyncImagePainter) {

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
    )

    Column(modifier = Modifier) {
        GreenColumn(
            space = 10,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {

            promo.overlineSmall?.also {
                Text(
                    text = it,
                    style = bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }

            promo.titleSmall?.also {
                Text(
                    it, style = headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }

            promo.textSmall?.also {
                Text(
                    it, style = bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }

        }

        GreenSpacer(80)

        PromoCta(promo, viewModel)
    }
}
