package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.x
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.blockstream.common.data.Promo
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import org.jetbrains.compose.resources.painterResource

@Composable
fun Promo(viewModel: GreenViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settingsManager.appSettingsStateFlow.collectAsStateWithLifecycle()
    val promoOrNull by viewModel.promo.collectAsStateWithLifecycle()

    AnimatedNullableVisibility(promoOrNull.takeIf { !settings.tor }) { promo ->
        LaunchedEffect(Unit) {
            viewModel.postEvent(Events.PromoImpression)
        }
        Promo(
            promo = promo,
            modifier = modifier,
            viewModel = viewModel
        )
    }
}

@Composable
fun Promo(
    promo: Promo,
    modifier: Modifier = Modifier,
    viewModel: GreenViewModel
) {
    val painter = rememberAsyncImagePainter(promo.imageSmall)

    val state by painter.state.collectAsStateWithLifecycle()

    if (promo.imageSmall.isBlank() || state is AsyncImagePainter.State.Success) {

        val imageIsShown = promo.imageSmall.isNotBlank() && state is AsyncImagePainter.State.Success

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

            GreenColumn(
                padding = 0,
                space = 8,
                modifier = Modifier.padding(
                    bottom = 16.dp
                )
            ) {

                Row {
                    Text(
                        text = promo.titleSmall ?: "",
                        style = titleMedium,
                        modifier = Modifier
                            .padding(
                                start = 16.dp,
                                end = if (imageIsShown) 0.dp else 36.dp,
                                top = 16.dp
                            )
                            .weight(1f)
                    )

                    if (promo.imageSmall.isNotBlank() && state is AsyncImagePainter.State.Success) {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier
                                .height(100.dp)
                                .align(Alignment.Top)
                        )
                    }
                }

                promo.textSmall?.also {
                    Text(
                        it, style = bodyMedium, modifier = Modifier
                            .padding(horizontal = 16.dp)
                    )
                }

                promo.ctaSmall?.also { ctaSmall ->
                    GreenButton(
                        text = ctaSmall,
                        size = GreenButtonSize.BIG,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        if (promo.isSmall) {
                            viewModel.postEvent(Events.PromoAction)
                        } else {
                            viewModel.postEvent(Events.PromoOpen)
                        }
                    }
                }
            }
        }
    }

}
