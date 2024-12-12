package com.blockstream.compose.screens.promo

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.Promo
import com.blockstream.common.events.Events
import com.blockstream.common.managers.PromoManager
import com.blockstream.common.models.promo.PromoViewModel
import com.blockstream.common.models.promo.PromoViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.VideoSurface
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.headlineLarge
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.ifTrue
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@Parcelize
data class PromoScreen(val promo: Promo, val greenWallet: GreenWallet?) : Screen, Parcelable {

    @Composable
    override fun Content() {
        Content(PaddingValues())
    }

    @Composable
    fun Content(innerPadding: PaddingValues) {
        val viewModel = koinScreenModel<PromoViewModel> {
            parametersOf(promo, greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        PromoScreen(innerPadding = innerPadding, viewModel = viewModel)
    }
}

@Composable
fun PromoScreen(
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: PromoViewModelAbstract
) {
    val promoManager = koinInject<PromoManager>()

    HandleSideEffect(viewModel)

    val promoOrNull by viewModel.promo.collectAsStateWithLifecycle()


    promoOrNull?.also { promo ->


        val imageLarge = remember {
            promo.imageLargeFile
        }

        val videoLarge = remember {
            promo.videoLargeFile
        }

        AnimatedNullableVisibility(
            value = videoLarge,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            VideoSurface(
                modifier = Modifier.fillMaxSize(),
                videoUri = it.filePath
            )
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)

        ) {
            GreenColumn(
                padding = 0,
                space = 10,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
                    .ifTrue(promo.layoutLarge == 1) {
                        offset(y = -32.dp)
                    }
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                val textAlign =
                    if (promo.layoutLarge == 0) TextAlign.Unspecified else TextAlign.Center

                imageLarge?.also {
                    AsyncImage(
                        model = it.filePath,
                        contentDescription = null,
                        modifier = Modifier.heightIn(max = 350.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }

                promo.overlineLarge?.also {
                    Text(
                        text = it,
                        style = bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = textAlign
                    )
                }

                promo.titleLarge?.also {
                    Text(
                        text = it,
                        style = if (promo.layoutLarge == 0) titleLarge else headlineLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = textAlign
                    )
                }

                promo.textLarge?.also {
                    Text(
                        it,
                        style = bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = textAlign
                    )
                }
            }

            promo.ctaLarge?.also { ctaSmall ->
                GreenButton(
                    text = ctaSmall,
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.postEvent(Events.PromoAction)
                }
            }
        }
    }
}

