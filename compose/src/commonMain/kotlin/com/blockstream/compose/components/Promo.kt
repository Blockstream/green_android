package com.blockstream.compose.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.x
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowSquareOut
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.md_theme_surfaceCircle
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.data.data.Promo
import com.blockstream.data.data.PromoCta
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val PROMO_HEIGHT = 128

@Composable
fun Promo(
    viewModel: GreenViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settingsManager.appSettingsStateFlow.collectAsStateWithLifecycle()
    val promos by viewModel.promos.collectAsStateWithLifecycle()

    if (!settings.tor && promos.isNotEmpty()) {
        PromoPager(
            promos = promos,
            modifier = modifier,
            onImpression = { viewModel.postEvent(Events.PromoImpression(it)) },
            onDismiss = { viewModel.postEvent(Events.PromoDismiss(it)) },
            onAction = { viewModel.postEvent(Events.PromoAction(it)) },
        )
    }
}

@Composable
private fun PromoPager(
    promos: List<Promo>,
    modifier: Modifier = Modifier,
    onImpression: (Promo) -> Unit,
    onDismiss: (Promo) -> Unit,
    onAction: (Promo) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { promos.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(PROMO_HEIGHT.dp),
            pageSpacing = 8.dp,
            key = { index -> promos[index].id },
        ) { page ->
            val promo = promos[page]

            LaunchedEffect(promo.id, pagerState.settledPage == page) {
                if (pagerState.settledPage == page) {
                    onImpression(promo)
                }
            }

            PromoCard(
                promo = promo,
                onDismiss = { onDismiss(promo) },
                onAction = { onAction(promo) },
            )
        }

        if (promos.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                promos.indices.forEach { page ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (pagerState.currentPage == page) whiteLow
                                else md_theme_surfaceCircle
                            )
                    )
                }
            }
        }
    }
}

@Composable
internal fun PromoCard(
    promo: Promo,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onAction: () -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(PROMO_HEIGHT.dp)) {
        val imageWidth = when {
            maxWidth <= 320.dp -> 96.dp
            maxWidth <= 600.dp -> 142.dp
            else -> 200.dp
        }
        val imagePainter = rememberAsyncImagePainter(
            model = promo.imageUrl?.takeIf { promo.hasValidImageUrl }
        )
        val imageState by imagePainter.state.collectAsStateWithLifecycle()
        var imageLoaded by remember(promo.imageUrl) { mutableStateOf(false) }

        LaunchedEffect(imageState) {
            if (imageState is AsyncImagePainter.State.Success) {
                imageLoaded = true
            }
        }

        val imageAlpha by animateFloatAsState(
            targetValue = if (imageLoaded) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "promoImageAlpha",
        )

        GreenCard(
            padding = 0,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxSize(),
            onClick = onAction,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        PromoText(
                            text = promo.title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                        )
                        PromoText(
                            text = promo.description,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                            color = whiteMedium,
                            maxLines = 3,
                        )
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val ctaTextMaxWidth = (this.maxWidth - 20.dp).coerceAtLeast(1.dp)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            PromoText(
                                text = promo.cta.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline,
                                ),
                                color = green,
                                maxLines = 1,
                                modifier = Modifier.widthIn(max = ctaTextMaxWidth),
                            )
                            Icon(
                                imageVector = PhosphorIcons.Regular.ArrowSquareOut,
                                contentDescription = null,
                                tint = green,
                                modifier = Modifier.padding(start = 4.dp).size(16.dp),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.width(imageWidth).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (imageState !is AsyncImagePainter.State.Success) {
                        PromoImageGlow()
                    }

                    if (imageState is AsyncImagePainter.State.Success) {
                        Image(
                            painter = imagePainter,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = imageAlpha },
                        )
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 1.dp).size(40.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.x),
                    contentDescription = "Dismiss",
                    tint = whiteLow,
                )
            }
        }
    }
}

@Composable
private fun PromoImageGlow() {
    val transition = rememberInfiniteTransition(label = "promoGlow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "promoGlowAlpha",
    )
    val glowScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "promoGlowScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    alpha = glowAlpha
                    scaleX = glowScale
                    scaleY = glowScale
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            Color.Transparent,
                        )
                    )
                )
        )
    }
}

@Composable
private fun PromoText(
    text: String,
    style: TextStyle,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.breakLongWords(),
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

// Chunks by Unicode code point (not UTF-16 char) so a surrogate pair emoji never lands mid-word.
private fun String.breakLongWords(): String =
    split(" ", "\n").joinToString(" ") { word ->
        val units = word.codePointUnits()
        if (units.size > 18) units.chunked(18).joinToString("\u200B") { it.joinToString("") } else word
    }

private fun String.codePointUnits(): List<String> {
    val units = mutableListOf<String>()
    var i = 0
    while (i < length) {
        val isSurrogatePair = this[i].isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()
        val unitLength = if (isSurrogatePair) 2 else 1
        units += substring(i, i + unitLength)
        i += unitLength
    }
    return units
}

@Composable
@Preview
private fun PromoCardPreview() {
    GreenPreview {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 1 line description
            PromoCard(
                promo = Promo(
                    id = "preview-1line",
                    title = "Meet Jade Core, $99",
                    description = "$10 off our newest Jade for app users only.",
                    cta = PromoCta(label = "Buy Now", url = "https://store.blockstream.com/"),
                    imageUrl = null,
                ),
            )
            // 3 line description, over-limit title and CTA (both should ellipsize, not hard-cut)
            PromoCard(
                promo = Promo(
                    id = "preview-3line",
                    title = "Meet Jade Core, our newest hardware wallet",
                    description = "This is the maximum ninety character description allowed by the promo content schema limit.",
                    cta = PromoCta(label = "Buy Now With A Long Label", url = "https://store.blockstream.com/"),
                    imageUrl = null,
                ),
            )
            PromoCard(
                promo = Promo(
                    id = "preview-longword-emoji",
                    title = "Supercalifragilisticexpialidocious",
                    description = "Anticonstitutionnellement🎉🚀😀 this word should break mid-word without mangling the emoji.",
                    cta = PromoCta(label = "Buy Now", url = "https://store.blockstream.com/"),
                    imageUrl = null,
                ),
            )
        }
    }
}
